package com.lagosrent.notification.infrastructure.provider.resend;

import com.lagosrent.notification.infrastructure.provider.EmailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@Slf4j
@ConditionalOnProperty(name = "notification.provider.active", havingValue = "resend")
public class ResendEmailProvider implements EmailProvider {
    private final String fromEmail;
    private final RestClient restClient;

    public ResendEmailProvider(
            @Value("${notification.provider.resend.api-key:re_dummy_api_key}") String apiKey,
            @Value("${notification.provider.resend.from:onboarding@resend.dev}") String fromEmail,
            @Value("${notification.provider.resend.base-url:https://api.resend.com}") String baseUrl
    ) {
        this.fromEmail = fromEmail;
        this.restClient = RestClient.builder()
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

        Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", List.of(command.to()),
                "subject", command.subject(),
                command.isHtml() ? "html" : "text", command.body()
        );

        try {
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
        }
        catch (Exception ex) {
            log.error("Failed to send email via Resend: {}", ex.getMessage(), ex);
            return new EmailDispatchResult(false, null, ex.getMessage());
        }

    }

    public record ResendResponse(String id) {}
}
