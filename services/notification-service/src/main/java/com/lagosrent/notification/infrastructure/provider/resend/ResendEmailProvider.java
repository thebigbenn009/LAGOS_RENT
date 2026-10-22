package com.lagosrent.notification.infrastructure.provider.resend;

import com.lagosrent.notification.infrastructure.provider.EmailProvider;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static java.time.Duration.ofMillis;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@Slf4j
@ConditionalOnProperty(name = "notification.provider.active", havingValue = "resend")
public class ResendEmailProvider implements EmailProvider {
    private final String fromEmail;
    private final RestClient restClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final Bulkhead resendBulkhead;
    private final RateLimiter resendRateLimiter;

    public ResendEmailProvider(
            @Value("${notification.provider.resend.api-key}") String apiKey,
            @Value("${notification.provider.resend.from}") String fromEmail,
            @Value("${notification.provider.resend.base-url}") String baseUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory,
            BulkheadRegistry bulkheadRegistry,
            RateLimiterRegistry rateLimiterRegistry
    ) {
        this.fromEmail = fromEmail;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.resendBulkhead = bulkheadRegistry.bulkhead("resend");
        this.resendRateLimiter = rateLimiterRegistry.rateLimiter("resend");

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(ofMillis(2000));
        factory.setReadTimeout(ofMillis(4000));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", APPLICATION_JSON_VALUE)
                .build();
    }


    @Override
    public String getProviderName() {
        return "RESEND";
    }

    @Override
    public EmailDispatchResult sendEmail(EmailDispatchCommand command) {
        log.info("Sending email via Resend to: {}", command.to());

        try {
            // Order: RateLimiter (with 500ms queueing) -> Bulkhead -> CircuitBreaker
            return resendRateLimiter.executeSupplier(() ->
                    resendBulkhead.executeSupplier(() ->
                            executeGuardedSend(command))
            );
        }
        catch (RequestNotPermitted ex) {
            log.warn("Resend email rate limit EXCEEDED (quota of 10 emails/sec reached). Delaying or failing dispatch to: {}", command.to());
            return new EmailDispatchResult(false, null, "Email provider rate limit reached (Resend quota full)");
        }
        catch (BulkheadFullException ex) {
            log.warn("Resend email bulkhead is FULL. Rejecting email to: {}", command.to());
            return new EmailDispatchResult(false, null, "Email provider concurrency limit reached (bulkhead full)");
        }
    }


    private EmailDispatchResult executeGuardedSend(EmailDispatchCommand command) {

        return circuitBreakerFactory.create("resend").run(
                () -> {
                    Map<String, Object> body = Map.of(
                            "from", fromEmail,
                            "to", List.of(command.to()),
                            "subject", command.subject(),
                            command.isHtml() ? "html" : "text", command.body()
                    );
                    ResendResponse response = restClient.post()
                            .uri("/emails")
                            .body(body)
                            .retrieve()
                            .body(ResendResponse.class);

                    if (response != null && response.id != null) {
                        log.info("Resend dispatch succeeded. Message ID: {}", response.id());
                        return new EmailDispatchResult(true, response.id, null);
                    }

                    return new EmailDispatchResult(false, null, "Resend returned empty response");
                },
                throwable -> {
                    log.error("Resend API call failed or circuit OPEN: {}", throwable.getMessage());
                    return new EmailDispatchResult(false, null, throwable.getMessage());
                }
        );
    }

    public record ResendResponse(String id) {}
}
