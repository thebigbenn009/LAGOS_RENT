package com.lagosrent.payment.infrastructure.provider.paystack;

import com.lagosrent.payment.infrastructure.provider.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static java.math.BigDecimal.valueOf;

@Component
@Slf4j
@ConditionalOnProperty(name = "payment.provider.active", havingValue = "paystack")
public class PaystackPaymentProvider implements PaymentProvider {
    private final RestClient restClient;
    private final String secretKey;

    public PaystackPaymentProvider(
            @Value("${payment.provider.paystack.secret-key}") String secretKey,
            @Value("${payment.provider.paystack.base-url:https://api.paystack.co}") String baseUrl) {
        this.secretKey = secretKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String getProviderName() {
        return "PAYSTACK";
    }

    @Override
    public PaymentInitResult initializePayment(PaymentInitCommand command) {
        log.info("Calling live Paystack initialize endpoint for reference: {}", command.internalReference());

        // Paystack expects amount in KOBO
        BigDecimal amountInKobo = command.amount().multiply(valueOf(100)).setScale(0, RoundingMode.HALF_UP);

        Map<String, Object> requestBody = Map.of(
                "reference", command.internalReference(),
                "amount", amountInKobo.toBigInteger(),
                "currency", command.currency(),
                "email", command.customerEmail(),
                "callback_url", command.callbackUrl()

        );

        try {
            PaystackInitApiResponse response = restClient.post()
                    .uri("/transaction/initialize")
                    .body(requestBody)
                    .retrieve()
                    .body(PaystackInitApiResponse.class);

            if (response != null && response.status() && response.data != null) {
                return new PaymentInitResult(
                        true,
                        response.data().reference,
                        response.data.authorization_url,
                        null
                );
            }
            return new PaymentInitResult(false, null, null, "Failed to obtain checkout URL from paystack");

        }
        catch (Exception ex) {
            log.error("Paystack initialization API call failed: {}", ex.getMessage(), ex);
            return new PaymentInitResult(false, null, null, ex.getMessage());
        }

    }

    @Override
    public PaymentVerificationResult verifyPayment(String providerReference) {
        log.info("Calling live Paystack verification endpoint for reference: {}", providerReference);
        try {
            PaystackVerifyApiResponse response = restClient.get()
                    .uri("/transaction/verify/{reference}", providerReference)
                    .retrieve()
                    .body(PaystackVerifyApiResponse.class);

            if (response != null && response.status() && response.data() != null) {
                // Convert returned Kobo back to Naira
                BigDecimal amountInNaira = response.data.amount()
                        .divide(valueOf(100), 2, RoundingMode.HALF_UP);

                return new PaymentVerificationResult(
                        true,
                        response.data.status,
                        response.data.reference,
                        response.data().reference(),
                        amountInNaira,
                        response.data.currency,
                        response.data.channel,
                        response.data.gateway_response
                );
            }
            return new PaymentVerificationResult(false, "failed", providerReference, null, null, "NGN", null, "Verification failed");
        }
        catch (Exception ex) {
            log.error("Paystack verification API call failed: {}", ex.getMessage(), ex);
            return new PaymentVerificationResult(false, "error", providerReference, null, null, "NGN", null, ex.getMessage());
        }
    }

    @Override
    public boolean validateWebhookSignature(String rawPayload, String signatureHeader) {
        if (signatureHeader == null || rawPayload == null) {
            return false;
        }
        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512Hmac.init(secretKeySpec);
            byte[] hash = sha512Hmac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);

            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().getBytes(StandardCharsets.UTF_8)
            );
        }
        catch (Exception e) {
            log.error("Failed to calculate HMAC-SHA512 signature: {}", e.getMessage());
            return false;
        }
    }

    // Paystack API Response records
    public record PaystackInitApiResponse(
            boolean status,
            String message,
            PaystackInitData data
    ) {}

    public record PaystackInitData(
            String authorization_url,
            String access_code,
            String reference
    ) {}

    public record PaystackVerifyApiResponse(
            boolean status,
            String message,
            PaystackVerifyData data
    ) {}

    public record PaystackVerifyData(
            BigDecimal amount,
            String currency,
            String reference,
            String status,
            String gateway_response,
            String channel
    ) {}
}
