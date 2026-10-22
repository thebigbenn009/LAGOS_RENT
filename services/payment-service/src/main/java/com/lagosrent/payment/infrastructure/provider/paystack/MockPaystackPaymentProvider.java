package com.lagosrent.payment.infrastructure.provider.paystack;

import com.lagosrent.payment.infrastructure.provider.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static java.util.UUID.randomUUID;

@Component
@Slf4j
@ConditionalOnProperty(name = "payment.provider.active", havingValue = "mock")
public class MockPaystackPaymentProvider implements PaymentProvider {
    private final String secretKey;

    public MockPaystackPaymentProvider(@Value("${payment.provider.paystack.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String getProviderName() {
        return "PAYSTACK";
    }

    @Override
    public PaymentInitResult initializePayment(PaymentInitCommand command) {
        log.info("Mock Paystack initializing checkout: ref={}, amount={} {}", command.internalReference(), command.amount(), command.currency());

        String providerRef = "pstk_" + randomUUID().toString().substring(0, 12);
        String checkoutUrl = "https://checkout.paystack.com/" + providerRef;

        return new PaymentInitResult(true, providerRef, checkoutUrl, null);
    }

    @Override
    public PaymentVerificationResult verifyPayment(String providerReference) {
        log.info("Mock Paystack verifying transaction: providerRef={}", providerReference);

        return new PaymentVerificationResult(
                true,
                "success",
                providerReference,
                null,
                null,
                "NGN",
                "card",
                null
        );
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


}
