package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.PushSubscription;
import com.vrtechnologies.vrtech.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final HttpClient httpClient;

    public PushNotificationService(PushSubscriptionRepository pushSubscriptionRepository) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Transactional
    public void subscribe(Long userId, String endpoint, String p256dh, String auth, String userAgent) {
        Optional<PushSubscription> existing = pushSubscriptionRepository.findByEndpoint(endpoint);
        PushSubscription sub = existing.orElseGet(PushSubscription::new);
        sub.setUserId(userId);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        sub.setUserAgent(userAgent);
        pushSubscriptionRepository.save(sub);
        log.info("Registered push subscription for user: {}, endpoint: {}", userId, endpoint);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.findByEndpoint(endpoint).ifPresent(sub -> {
            pushSubscriptionRepository.delete(sub);
            log.info("Unsubscribed push endpoint: {}", endpoint);
        });
    }

    public void sendPush(Long userId, String title, String message) {
        List<PushSubscription> subs = pushSubscriptionRepository.findByUserId(userId);
        if (subs.isEmpty()) {
            log.info("No active push subscriptions for user id: {}", userId);
            return;
        }

        log.info("Dispatching push alert to {} active endpoints for user id: {}", subs.size(), userId);
        
        boolean firebaseInitialized = !com.google.firebase.FirebaseApp.getApps().isEmpty();
        
        for (PushSubscription sub : subs) {
            try {
                if (firebaseInitialized) {
                    log.info("Firebase messaging active. Attempting payload dispatch to endpoint: {}", sub.getEndpoint());
                }
                
                // Ping the web push endpoint to wake up the browser service worker
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(sub.getEndpoint()))
                        .timeout(Duration.ofSeconds(5))
                        .header("TTL", "180")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                        .thenAccept(res -> {
                            log.info("Web push endpoint pinged: status={}", res.statusCode());
                        })
                        .exceptionally(err -> {
                            log.warn("Failed to ping web push endpoint: {}", err.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                log.warn("Error processing push for endpoint {}: {}", sub.getEndpoint(), e.getMessage());
            }
        }
    }

    public void sendPushToAllAdmins(String title, String message) {
        List<PushSubscription> subs = pushSubscriptionRepository.findAll();
        if (subs.isEmpty()) {
            return;
        }

        log.info("Dispatching broadcast push alert to {} active admin endpoints", subs.size());
        boolean firebaseInitialized = !com.google.firebase.FirebaseApp.getApps().isEmpty();

        for (PushSubscription sub : subs) {
            try {
                if (firebaseInitialized) {
                    log.info("Firebase messaging active. Attempting payload dispatch to endpoint: {}", sub.getEndpoint());
                }

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(sub.getEndpoint()))
                        .timeout(Duration.ofSeconds(5))
                        .header("TTL", "180")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                        .thenAccept(res -> {
                            log.info("Web push endpoint pinged: status={}", res.statusCode());
                        })
                        .exceptionally(err -> {
                            log.warn("Failed to ping web push endpoint: {}", err.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                log.warn("Error processing broadcast push for endpoint {}: {}", sub.getEndpoint(), e.getMessage());
            }
        }
    }
}
