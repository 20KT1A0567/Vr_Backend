package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.NotificationLog;
import com.vrtechnologies.vrtech.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository notificationLogRepository;
    private final EmailService emailService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.notifications.worker.enabled:true}")
    private boolean workerEnabled;

    @Value("${app.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    @Value("${app.whatsapp.provider-url:}")
    private String whatsappProviderUrl;

    @Value("${app.whatsapp.bearer-token:}")
    private String whatsappBearerToken;

    public NotificationService(NotificationLogRepository notificationLogRepository, EmailService emailService, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.notificationLogRepository = notificationLogRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    public void logOrderEvent(String eventType, CustomerOrder order, String subject, String message) {
        if (order == null) {
            return;
        }
        log(eventType, "EMAIL", order.getContactEmail(), subject, message, order.getId());
        log(eventType, "WHATSAPP", order.getContactPhone(), subject, message, order.getId());
    }

    public List<NotificationLog> latest() {
        return notificationLogRepository.findTop100ByOrderByCreatedAtDescIdDesc();
    }

    public NotificationLog markRead(Long id) {
        NotificationLog entry = notificationLogRepository.findById(id)
                .orElseThrow(() -> new com.vrtechnologies.vrtech.exception.ResourceNotFoundException("Notification not found"));
        entry.setRead(true);
        return notificationLogRepository.save(entry);
    }

    public long unreadCount() {
        return notificationLogRepository.countByIsReadFalse();
    }

    public void markAllRead() {
        List<NotificationLog> entries = notificationLogRepository.findTop100ByOrderByCreatedAtDescIdDesc();
        entries.forEach(entry -> entry.setRead(true));
        notificationLogRepository.saveAll(entries);
    }

    public NotificationLog log(String eventType, String channel, String recipient, String subject, String message, Long orderId) {
        if (recipient == null || recipient.isBlank()) {
            return null;
        }
        NotificationLog entry = new NotificationLog();
        entry.setEventType(eventType);
        entry.setChannel(channel);
        entry.setRecipient(recipient);
        entry.setSubject(subject);
        entry.setMessage(message);
        entry.setStatus("QUEUED");
        entry.setOrderId(orderId);
        entry.setAttempts(0);
        entry.setMaxAttempts(3);
        entry.setNextAttemptAt(LocalDateTime.now());
        return notificationLogRepository.save(entry);
    }

    public NotificationLog logInApp(String eventType, String subject, String message, Long orderId) {
        NotificationLog entry = new NotificationLog();
        entry.setEventType(eventType);
        entry.setChannel("IN_APP");
        entry.setRecipient("admins");
        entry.setSubject(subject);
        entry.setMessage(message);
        entry.setStatus("DELIVERED");
        entry.setOrderId(orderId);
        entry.setSentAt(LocalDateTime.now());
        entry.setNextAttemptAt(null);
        return notificationLogRepository.save(entry);
    }

    @Scheduled(fixedDelayString = "${app.notifications.worker.delay-ms:30000}")
    @Transactional
    public void processQueuedNotifications() {
        if (!workerEnabled) {
            return;
        }
        List<NotificationLog> due = notificationLogRepository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAscIdAsc(
                List.of("QUEUED", "RETRY"),
                LocalDateTime.now()
        );
        for (NotificationLog entry : due) {
            deliver(entry);
        }
    }

    private void deliver(NotificationLog entry) {
        entry.setAttempts((entry.getAttempts() == null ? 0 : entry.getAttempts()) + 1);
        try {
            if ("EMAIL".equalsIgnoreCase(entry.getChannel())) {
                deliverEmail(entry);
            } else if ("WHATSAPP".equalsIgnoreCase(entry.getChannel())) {
                deliverWhatsApp(entry);
            } else {
                markUnsupported(entry, "Unsupported notification channel: " + entry.getChannel());
            }
        } catch (Exception exception) {
            markFailedOrRetry(entry, exception.getMessage());
            log.warn("Notification {} delivery failed: {}", entry.getId(), exception.getMessage());
        }
        notificationLogRepository.save(entry);
    }

    private void deliverEmail(NotificationLog entry) {
        if (!emailService.isConfigured()) {
            markUnsupported(entry, "Email sender is not configured");
            return;
        }
        emailService.sendHtml(entry.getRecipient(), entry.getSubject(), toHtml(entry));
        entry.setStatus("SENT");
        entry.setSentAt(LocalDateTime.now());
        entry.setLastError(null);
        entry.setNextAttemptAt(null);
    }

    private void deliverWhatsApp(NotificationLog entry) throws Exception {
        if (!whatsappEnabled || whatsappProviderUrl == null || whatsappProviderUrl.isBlank()) {
            markUnsupported(entry, "WhatsApp provider is not configured");
            return;
        }
        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "to", entry.getRecipient(),
                "subject", entry.getSubject() == null ? "" : entry.getSubject(),
                "message", entry.getMessage() == null ? "" : entry.getMessage(),
                "eventType", entry.getEventType() == null ? "" : entry.getEventType()
        ));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(whatsappProviderUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));
        if (whatsappBearerToken != null && !whatsappBearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + whatsappBearerToken);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("WhatsApp provider returned HTTP " + response.statusCode());
        }
        entry.setStatus("SENT");
        entry.setSentAt(LocalDateTime.now());
        entry.setLastError(null);
        entry.setNextAttemptAt(null);
    }

    private void markUnsupported(NotificationLog entry, String message) {
        entry.setStatus("SKIPPED");
        entry.setLastError(message);
        entry.setNextAttemptAt(null);
    }

    private void markFailedOrRetry(NotificationLog entry, String message) {
        entry.setLastError(message);
        int attempts = entry.getAttempts() == null ? 0 : entry.getAttempts();
        int maxAttempts = entry.getMaxAttempts() == null ? 3 : entry.getMaxAttempts();
        if (attempts >= maxAttempts) {
            entry.setStatus("FAILED");
            entry.setNextAttemptAt(null);
            return;
        }
        entry.setStatus("RETRY");
        entry.setNextAttemptAt(LocalDateTime.now().plusMinutes(Math.max(1, attempts * 5L)));
    }

    private String toHtml(NotificationLog entry) {
        String title = escapeHtml(entry.getSubject() == null ? "VR Technologies update" : entry.getSubject());
        String body = escapeHtml(entry.getMessage() == null ? "" : entry.getMessage()).replace("\n", "<br>");
        return "<div style=\"font-family:Arial,sans-serif;line-height:1.6;color:#0f172a\">"
                + "<h2 style=\"margin:0 0 12px;color:#123b8f\">" + title + "</h2>"
                + "<p>" + body + "</p>"
                + "<p style=\"margin-top:24px;color:#64748b;font-size:13px\">VR Technologies</p>"
                + "</div>";
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
