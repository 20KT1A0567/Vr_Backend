package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.service.PushNotificationService;
import com.vrtechnologies.vrtech.service.UserContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications/push")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;
    private final UserContextService userContextService;

    public PushNotificationController(
            PushNotificationService pushNotificationService,
            UserContextService userContextService
    ) {
        this.pushNotificationService = pushNotificationService;
        this.userContextService = userContextService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        Long userId = null;
        try {
            User user = userContextService.getCurrentUser();
            if (user != null) {
                userId = user.getId();
            }
        } catch (Exception e) {
            // Ignore unauthenticated or anonymous users
        }

        String endpoint = payload.get("endpoint");
        String p256dh = payload.get("p256dh");
        String auth = payload.get("auth");

        if (endpoint == null || endpoint.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Endpoint is required"));
        }

        pushNotificationService.subscribe(userId, endpoint, p256dh, auth, userAgent);
        return ResponseEntity.ok(Map.of("success", true, "message", "Successfully subscribed to push notifications"));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody Map<String, String> payload) {
        String endpoint = payload.get("endpoint");
        if (endpoint == null || endpoint.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Endpoint is required"));
        }

        pushNotificationService.unsubscribe(endpoint);
        return ResponseEntity.ok(Map.of("success", true, "message", "Successfully unsubscribed from push notifications"));
    }
}
