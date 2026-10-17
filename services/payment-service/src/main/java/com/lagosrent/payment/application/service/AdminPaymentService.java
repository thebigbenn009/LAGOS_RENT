package com.lagosrent.payment.application.service;

import com.lagosrent.payment.api.dto.AdminReprocessWebhookRequest;
import com.lagosrent.payment.api.dto.AdminTransactionResponse;
import com.lagosrent.payment.api.dto.AdminWebhookResponse;
import com.lagosrent.payment.application.exception.ResourceNotFoundException;
import com.lagosrent.payment.domain.model.ProviderWebhook;
import com.lagosrent.payment.domain.model.WebhookStatus;
import com.lagosrent.payment.infrastructure.persistence.PaymentTransactionRepository;
import com.lagosrent.payment.infrastructure.persistence.ProviderWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.lagosrent.payment.domain.model.WebhookStatus.PROCESSED;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentService {
    private final ProviderWebhookRepository webhookRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentWebhookService webhookService;

    @Transactional(readOnly = true)
    public Page<AdminWebhookResponse> getWebhooks(WebhookStatus status, Pageable pageable) {
        Page<ProviderWebhook> webhooks = (status != null)
                ? webhookRepository.findAllByStatus(status, pageable)
                : webhookRepository.findAll(pageable);

        return webhooks.map(AdminWebhookResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> getTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(AdminTransactionResponse::from);
    }

    @Transactional
    public AdminWebhookResponse reprocessWebhook(
            UUID webhookId,
            String adminKeycloakId,
            AdminReprocessWebhookRequest request
    ) {
        ProviderWebhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("ProviderWebhook" + webhookId));

        if (webhook.getStatus() == PROCESSED) {
            throw new IllegalStateException("Webhook is already marked PROCESSED. Reprocessing settled transactions is prohibited.");
        }

        log.info("Admin {} re-processing webhook {}. Reason: {}", adminKeycloakId, webhookId, request.reason());

        // Delegate to core webhook processing engine
        webhookService.processPaystackWebhook(webhook.getPayload(), webhook.getSignatureHeader());

        // Refresh entity state
        webhook = webhookRepository.findById(webhookId).orElseThrow();
        return AdminWebhookResponse.from(webhook);

    }
}
