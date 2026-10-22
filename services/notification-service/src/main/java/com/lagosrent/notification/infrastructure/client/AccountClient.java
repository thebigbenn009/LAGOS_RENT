package com.lagosrent.notification.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static java.time.Duration.ofMillis;

@Component
@Slf4j
public class AccountClient {
    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager clientManager;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public AccountClient(
            @Value("${account-service.url}") String accountServiceUrl,
            OAuth2AuthorizedClientManager clientManager,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory

    ) {
        this.clientManager = clientManager;
        this.circuitBreakerFactory = circuitBreakerFactory;

        // Hardened Request Factory with strict socket timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(ofMillis(1500));
        factory.setReadTimeout(ofMillis(2500));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(accountServiceUrl)
                .build();
    }

    /**
     * Retrieves user profile from account-service protected by Spring Cloud CircuitBreaker.
     */

    public UserProfileDto getUserProfile(String keycloakId) {
        return circuitBreakerFactory.create("accountService").run(
                () -> {
                    String token = getServiceAccountToken();
                    UserProfileDto profile = restClient.get()
                            .uri("/api/v1/accounts/user/{keycloakId}", keycloakId)
                            .header("Authorization", "Bearer " + token)
                            .retrieve()
                            .body(UserProfileDto.class);
                    if (profile == null || profile.email() == null || profile.email().isBlank()) {
                        throw new IllegalStateException("Account profile missing mandatory email for keycloakId: " + keycloakId);
                    }
                    return profile;
                },
                throwable -> {
                    log.error("AccountService call failed or circuit open for keycloakId: {}. Error: {}", keycloakId, throwable.getMessage());
                    throw new IllegalStateException("Account Service is currently unavailable: " + throwable.getMessage(), throwable);
                }
        );

    }

    private String getServiceAccountToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId("account-service-client")
                .principal("notification-service")
                .build();

        OAuth2AuthorizedClient client = clientManager.authorize(request);
        if (client == null) {
            throw new IllegalStateException("Failed to obtain Service Account token from Keycloak for notification-service");
        }
        return client.getAccessToken().getTokenValue();
    }

    public record UserProfileDto(
            String keycloakId,
            String email,
            String fullName,
            String phoneNumber
    ) {}
}
