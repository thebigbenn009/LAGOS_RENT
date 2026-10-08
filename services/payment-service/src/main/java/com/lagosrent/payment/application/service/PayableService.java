package com.lagosrent.payment.application.service;

import com.lagosrent.payment.api.dto.CreatePayableRequest;
import com.lagosrent.payment.api.dto.InitializePaymentResponse;
import com.lagosrent.payment.api.dto.PayableResponse;
import com.lagosrent.payment.application.exception.ResourceNotFoundException;
import com.lagosrent.payment.domain.model.Payable;
import com.lagosrent.payment.domain.model.PaymentAttempt;
import com.lagosrent.payment.infrastructure.persistence.PayableRepository;
import com.lagosrent.payment.infrastructure.persistence.PaymentAttemptRepository;
import com.lagosrent.payment.infrastructure.provider.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.lagosrent.payment.domain.model.PayableStatus.CREATED;
import static com.lagosrent.payment.domain.model.PayableStatus.PENDING;
import static com.lagosrent.payment.infrastructure.provider.PaymentProvider.PaymentInitCommand;
import static com.lagosrent.payment.infrastructure.provider.PaymentProvider.PaymentInitResult;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayableService {
    private final PayableRepository payableRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentProvider paymentProvider;

    @Transactional
    public PayableResponse createPayable(String tenantKeycloakId, CreatePayableRequest request) {
        payableRepository.findByApplicationId(request.applicationId()).ifPresent(p -> {
            throw new IllegalStateException("Payable already exists for application: " + request.applicationId());
        });

        Payable payable = new Payable(
                request.applicationId(),
                request.listingId(),
                tenantKeycloakId,
                request.landlordKeycloakId(),
                request.reservationReference(),
                request.totalAmount(),
                request.currency() != null ? request.currency() : "NGN",
                request.dueDate() != null ? request.dueDate() : now().plusSeconds(172800) // 48hrs
        );

        Payable saved = payableRepository.save(payable);
        log.info("Created payable id={} for application={}", saved.getId(), saved.getApplicationId());
        return PayableResponse.from(saved);
    }

    @Transactional
    public InitializePaymentResponse initializeCheckout(UUID payableId, String tenantKeycloakId, String tenantEmail, String idempotencyKey) {
        Payable payable = payableRepository.findById(payableId)
                .orElseThrow(() -> new ResourceNotFoundException("payable", payableId));

        if (!payable.getTenantKeycloakId().equals(tenantKeycloakId)) {
            throw new IllegalArgumentException("You do not own this payable");
        }

        if (payable.getStatus() != CREATED && payable.getStatus() != PENDING) {
            throw new IllegalStateException("Cannot checkout payable in status: " + payable.getStatus());
        }

        // Idempotency check: if this key was already used, return existing attempt
        if (idempotencyKey != null) {
            var existing = attemptRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                PaymentAttempt attempt = existing.get();
                return new InitializePaymentResponse(
                        attempt.getId(),
                        payable.getId(),
                        attempt.getInternalReference(),
                        attempt.getProviderReference(),
                        attempt.getAuthorizationUrl(),
                        attempt.getAmount(),
                        attempt.getCurrency()
                );
            }
        }

        String internalRef = "PAY-TX-" + randomUUID().toString().substring(0, 10).toUpperCase();
        String safeIdempotencyKey = idempotencyKey != null ? idempotencyKey : randomUUID().toString();

        PaymentInitCommand command = new PaymentInitCommand(
                internalRef,
                payable.getTotalAmount(),
                payable.getCurrency(),
                tenantEmail,
                "http://localhost:8080/checkout/callback"
        );

        PaymentInitResult initResult = paymentProvider.initializePayment(command);
        if (!initResult.success()) {
            throw new RuntimeException("Payment provider initialization failed: " + initResult.errorMessage());
        }

        PaymentAttempt attempt = new PaymentAttempt(
                payable,
                internalRef,
                safeIdempotencyKey,
                paymentProvider.getProviderName(),
                payable.getTotalAmount(),
                payable.getCurrency(),
                initResult.authorizationUrl()
        );

        attempt.setProviderReference(initResult.providerReference());
        attemptRepository.save(attempt);

        payable.setStatus(PENDING);
        payableRepository.save(payable);

        return new InitializePaymentResponse(
                attempt.getId(),
                payable.getId(),
                internalRef,
                initResult.providerReference(),
                initResult.authorizationUrl(),
                payable.getTotalAmount(),
                payable.getCurrency()
        );
    }

    @Transactional(readOnly = true)
    public PayableResponse getPayableById(UUID payableId) {
        return payableRepository.findById(payableId)
                .map(PayableResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Payable", payableId));
    }

    @Transactional(readOnly = true)
    public List<PayableResponse> getTenantPayables(String tenantKeycloakId) {
        return payableRepository.findByTenantKeycloakId(tenantKeycloakId).stream()
                .map(PayableResponse::from)
                .toList();

    }
}
