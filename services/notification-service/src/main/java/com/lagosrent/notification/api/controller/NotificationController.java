package com.lagosrent.notification.api.controller;

import com.lagosrent.notification.domain.model.NotificationDelivery;
import com.lagosrent.notification.infrastructure.persistence.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationDeliveryRepository deliveryRepository;

    @GetMapping("/my-history")
    public ResponseEntity<List<NotificationDelivery>> getMyNotificationHistory(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        if (userEmail == null || userEmail.isBlank()) {
            userEmail = jwt.getClaimAsString("preferred_username") + "@lagosrent.internal";
        }
        return ResponseEntity.ok(deliveryRepository.findByRecipient(userEmail));
    }
}
