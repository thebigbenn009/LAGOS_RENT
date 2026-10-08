package com.lagosrent.payment.application.service;

import com.lagosrent.payment.domain.model.*;
import com.lagosrent.payment.infrastructure.messaging.payload.PaymentSucceededPayload;
import com.lagosrent.payment.infrastructure.persistence.*;
import com.lagosrent.payment.infrastructure.provider.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Year;

import static com.lagosrent.payment.domain.model.PayableStatus.PAID;
import static com.lagosrent.payment.domain.model.PaymentStatus.SUCCEEDED;
import static com.lagosrent.payment.domain.model.WebhookStatus.*;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {
    private final PaymentProvider paymentProvider;
    private final ProviderWebhookRepository webhookRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PayableRepository payableRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final ReceiptRepository receiptRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    /**
     * Strictly verifies and processes incoming payment provider webhooks.
     */
    @Transactional
    public void processPaystackWebhook(String rawPayload, String signatureHeader) {
        // 1. Signature Verification
        if (!paymentProvider.validateWebhookSignature(rawPayload, signatureHeader)) {
            log.error("Invalid webhook signature received");
            throw new SecurityException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Malformed JSON in webhook", e);
        }

        String eventType = root.path("event").asString();
        JsonNode dataNode = root.path("data");
        String providerReference = dataNode.path("reference").asString();
        String internalReference = dataNode.path("reference").asString();
        String status = dataNode.path("status").asString();
        BigDecimal amountPaid = dataNode.path("amount").asDecimal();
        String currency = dataNode.path("currency").asString("NGN");
        String channel = dataNode.path("channel").asString("card");

        // Normalize kobo to naira if sent in minor units
        if (amountPaid != null && amountPaid.compareTo(new BigDecimal("10000000")) > 0) {
            amountPaid = amountPaid.divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        }

        // 2. Idempotency Check
        var existingWebhook = webhookRepository.findByProviderNameAndEventTypeAndProviderReference(paymentProvider.getProviderName(), eventType,
                providerReference);

        if (existingWebhook.isPresent() && existingWebhook.get().getStatus() == PROCESSED) {
            log.info("Duplicate webhook delivery detected and safely ignored: event={}, providerRef={}", eventType, providerReference);
            return;
        }

        ProviderWebhook webhookRecord = existingWebhook.orElseGet(() ->
                new ProviderWebhook(paymentProvider.getProviderName(), eventType, providerReference, rawPayload, signatureHeader)
        );

        if (!"charge.success".equalsIgnoreCase(eventType) && !"success".equalsIgnoreCase(status)) {
            webhookRecord.setStatus(IGNORED);
            webhookRecord.setProcessedAt(now());
            webhookRepository.save(webhookRecord);
            return;
        }

        // 3. Match internal PaymentAttempt
        PaymentAttempt attempt = attemptRepository.findByInternalReference(internalReference).orElse(null);

        if (attempt == null) {
            log.error("Webhook references unknown internal transaction: ref={}", internalReference);
            webhookRecord.setStatus(FAILED);
            webhookRecord.setProcessingError("Unknown internal transaction reference");
            webhookRepository.save(webhookRecord);
            return;
        }

        Payable payable = attempt.getPayable();

        // 4. Validate amount and currency against server-side payable
        if (payable.getTotalAmount().compareTo(amountPaid) != 0) {
            log.error("Amount mismatch! Expected={}, Paid={}", payable.getTotalAmount(), amountPaid);
            webhookRecord.setStatus(FAILED);
            webhookRecord.setProcessingError("Amount mismatch between payable and provider charge");
            webhookRepository.save(webhookRecord);
            return;
        }

        // 5. Update Statuses and Persist Immutable Financial Records
        attempt.setStatus(SUCCEEDED);
        attemptRepository.save(attempt);

        payable.setStatus(PAID);
        payableRepository.save(payable);

        PaymentTransaction transaction = new PaymentTransaction(
                payable,
                attempt,
                internalReference,
                providerReference,
                paymentProvider.getProviderName(),
                amountPaid,
                currency,
                channel,
                now()
        );
        transactionRepository.save(transaction);

        // 6. Generate Official Sequential Receipt
        String receiptNumber = "RCP-" + Year.now().getValue() + "-" + randomUUID().toString().substring(0, 8).toUpperCase();
        String receiptDataJson;
        try {
            receiptDataJson = objectMapper.writeValueAsString(dataNode);
        }
        catch (Exception e) {
            receiptDataJson = "{}";
        }

        Receipt receipt = new Receipt(
                transaction,
                payable,
                receiptNumber,
                amountPaid,
                currency,
                payable.getTenantKeycloakId(),
                payable.getLandlordKeycloakId(),
                receiptDataJson
        );
        receiptRepository.save(receipt);

        // 7. Atomically Emit Domain Event to RabbitMQ via Outbox
        outboxService.recordEvent(
                "payable",
                payable.getId().toString(),
                1L,
                "payment.succeeded.v1",
                new PaymentSucceededPayload(
                        transaction.getId(),
                        payable.getApplicationId(),
                        payable.getTenantKeycloakId(),
                        payable.getLandlordKeycloakId(),
                        amountPaid,
                        currency,
                        providerReference,
                        now()
                ),
                null,
                null
        );

        webhookRecord.setStatus(PROCESSED);
        webhookRecord.setProcessedAt(now());
        webhookRepository.save(webhookRecord);

        log.info("Payment confirmed, receipt issued: receiptNo={}, payableId={}", receiptNumber, payable.getId());

    }
}
