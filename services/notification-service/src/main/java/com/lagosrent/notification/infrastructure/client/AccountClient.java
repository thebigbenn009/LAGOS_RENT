package com.lagosrent.notification.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class AccountClient {
    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager clientManager;

    public AccountClient(
            @Value("${account-service.url:http://localhost:8081}") String accountServiceUrl,
            OAuth2AuthorizedClientManager clientManager
    ) {
        this.clientManager = clientManager;
        this.restClient = RestClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }

    public UserProfileDto getUserProfile(String keycloakId) {
        String token = getServiceAccountToken();
        return restClient.get()
                .uri("/api/v1/accounts/user/{keycloakId}", keycloakId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(UserProfileDto.class);

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
