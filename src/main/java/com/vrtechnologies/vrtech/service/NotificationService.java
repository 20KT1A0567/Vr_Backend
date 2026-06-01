package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.OrderItem;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.NotificationLog;
import com.vrtechnologies.vrtech.repository.NotificationLogRepository;
import com.vrtechnologies.vrtech.repository.CustomerOrderRepository;
import com.vrtechnologies.vrtech.dto.event.SystemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final NotificationLogRepository notificationLogRepository;
    private final EmailService emailService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final SseEmitterService sseEmitterService;
    private final PushNotificationService pushNotificationService;
    private final CustomerOrderRepository customerOrderRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.notifications.worker.enabled:true}")
    private boolean workerEnabled;

    @Value("${app.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    @Value("${app.whatsapp.provider-url:}")
    private String whatsappProviderUrl;

    @Value("${app.whatsapp.bearer-token:}")
    private String whatsappBearerToken;

    @Value("${app.website.url:http://localhost:5173}")
    private String websiteUrl;

    public NotificationService(
            NotificationLogRepository notificationLogRepository,
            EmailService emailService,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            SseEmitterService sseEmitterService,
            PushNotificationService pushNotificationService,
            CustomerOrderRepository customerOrderRepository
    ) {
        this.notificationLogRepository = notificationLogRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.sseEmitterService = sseEmitterService;
        this.pushNotificationService = pushNotificationService;
        this.customerOrderRepository = customerOrderRepository;
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
        if ("EMAIL".equalsIgnoreCase(channel) && !isDeliverableEmail(recipient)) {
            log.info("Skipping email notification {} for non-email recipient {}", eventType, recipient);
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
        NotificationLog saved = notificationLogRepository.save(entry);
        
        try {
            SystemEvent event = SystemEvent.builder()
                    .eventType("NOTIFICATION_CREATED")
                    .title("New Notification")
                    .message(saved.getSubject() != null ? saved.getSubject() : saved.getMessage())
                    .severity("INFO")
                    .payload(java.util.Map.of(
                            "id", saved.getId(),
                            "channel", saved.getChannel(),
                            "eventType", saved.getEventType(),
                            "subject", saved.getSubject() != null ? saved.getSubject() : "",
                            "message", saved.getMessage() != null ? saved.getMessage() : ""
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();
            sseEmitterService.broadcast(event);
        } catch (Exception e) {
            log.error("Failed to broadcast notification event: {}", e.getMessage());
        }
        
        if (saved != null && isSystemAlert(eventType)) {
            pushNotificationService.sendPushToAllAdmins(saved.getSubject(), saved.getMessage());
        }
        
        return saved;
    }

    private boolean isDeliverableEmail(String recipient) {
        String normalized = recipient.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        int atIndex = normalized.lastIndexOf('@');
        if (atIndex < 0 || atIndex == normalized.length() - 1) {
            return false;
        }
        String domain = normalized.substring(atIndex + 1);
        return !domain.endsWith(".local");
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
        NotificationLog saved = notificationLogRepository.save(entry);
        
        try {
            SystemEvent event = SystemEvent.builder()
                    .eventType("NOTIFICATION_CREATED")
                    .title("New In-App Notification")
                    .message(saved.getSubject() != null ? saved.getSubject() : saved.getMessage())
                    .severity("INFO")
                    .payload(java.util.Map.of(
                            "id", saved.getId(),
                            "channel", saved.getChannel(),
                            "eventType", saved.getEventType(),
                            "subject", saved.getSubject() != null ? saved.getSubject() : "",
                            "message", saved.getMessage() != null ? saved.getMessage() : ""
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();
            sseEmitterService.broadcast(event);
        } catch (Exception e) {
            log.error("Failed to broadcast in-app notification event: {}", e.getMessage());
        }
        
        if (saved != null && isSystemAlert(eventType)) {
            pushNotificationService.sendPushToAllAdmins(saved.getSubject(), saved.getMessage());
        }
        
        return saved;
    }

    private boolean isSystemAlert(String eventType) {
        if (eventType == null) return false;
        String upper = eventType.toUpperCase();
        return upper.contains("ORDER") || upper.contains("SUPPORT") || upper.contains("SECURITY") 
            || upper.contains("PAYMENT") || upper.contains("CART") || upper.contains("STOCK");
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
        if (!isDeliverableEmail(entry.getRecipient())) {
            markUnsupported(entry, "Recipient is not a deliverable email address");
            return;
        }
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
        if (entry.getOrderId() != null) {
            try {
                java.util.Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(entry.getOrderId());
                if (orderOpt.isPresent()) {
                    return buildOrderEmailHtml(orderOpt.get(), entry);
                }
            } catch (Exception e) {
                log.error("Failed to build detailed order HTML email for order ID {}: {}", entry.getOrderId(), e.getMessage());
            }
        }
        String title = escapeHtml(entry.getSubject() == null ? "VR Technologies update" : entry.getSubject());
        String body = escapeHtml(entry.getMessage() == null ? "" : entry.getMessage()).replace("\n", "<br>");
        return "<div style=\"font-family:Arial,sans-serif;line-height:1.6;color:#0f172a\">"
                + "<h2 style=\"margin:0 0 12px;color:#123b8f\">" + title + "</h2>"
                + "<p>" + body + "</p>"
                + "<p style=\"margin-top:24px;color:#64748b;font-size:13px\">VR Technologies</p>"
                + "</div>";
    }

    private String buildOrderEmailHtml(CustomerOrder order, NotificationLog entry) {
        String subject = escapeHtml(entry.getSubject() != null ? entry.getSubject() : "Order Notification");
        String customerName = escapeHtml(order.getContactName() != null ? order.getContactName() : "Customer");
        String messageBody = escapeHtml(entry.getMessage() != null ? entry.getMessage() : "");
        String orderNumber = escapeHtml(order.getOrderNumber() != null ? order.getOrderNumber() : "ORD-" + order.getId());
        String contactPhone = order.getContactPhone() != null ? order.getContactPhone() : "";
        String trackingUrl = websiteUrl + "/track?orderNumber=" + java.net.URLEncoder.encode(orderNumber, java.nio.charset.StandardCharsets.UTF_8)
                + "&phone=" + java.net.URLEncoder.encode(contactPhone, java.nio.charset.StandardCharsets.UTF_8);

        String orderDate = "";
        if (order.getCreatedAt() != null) {
            try {
                orderDate = order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
            } catch (Exception e) {
                orderDate = order.getCreatedAt().toString();
            }
        }

        String deliveryType = order.getDeliveryType() != null ? order.getDeliveryType().name() : "DELIVERY";
        String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING";
        String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "CASH";

        // Build items list
        StringBuilder itemsHtml = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String title = item.getProduct() != null ? item.getProduct().getTitle() : "Product";
                String priceStr = "₹" + (item.getPriceAtTime() != null ? item.getPriceAtTime().setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
                BigDecimal totalItemPrice = (item.getPriceAtTime() != null && item.getQuantity() != null)
                        ? item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity()))
                        : BigDecimal.ZERO;
                String totalItemPriceStr = "₹" + totalItemPrice.setScale(2, java.math.RoundingMode.HALF_UP);

                itemsHtml.append("<tr>")
                         .append("<td style=\"padding: 12px 0; border-bottom: 1px solid #f1f5f9; font-size: 14px; color: #334155;\">")
                         .append("<span style=\"font-weight: 600; color: #0f172a;\">").append(escapeHtml(title)).append("</span>");

                if (item.getProductVariant() != null && item.getProductVariant().getAttributeValues() != null) {
                    String variantSpecs = item.getProductVariant().getAttributeValues().stream()
                            .map(av -> av.getAttribute().getName() + ": " + av.getValue())
                            .collect(java.util.stream.Collectors.joining(" | "));
                    if (!variantSpecs.isEmpty()) {
                        itemsHtml.append("<div style=\"font-size: 12px; color: #1e3a8a; font-weight: 700; margin-top: 2px;\">")
                                 .append(escapeHtml(variantSpecs))
                                 .append("</div>");
                    }
                }

                itemsHtml.append("</td>")
                         .append("<td align=\"center\" style=\"padding: 12px 0; border-bottom: 1px solid #f1f5f9; font-size: 14px; color: #475569;\">")
                         .append(item.getQuantity())
                         .append("</td>")
                         .append("<td align=\"right\" style=\"padding: 12px 0; border-bottom: 1px solid #f1f5f9; font-size: 14px; color: #0f172a; font-weight: 600;\">")
                         .append(totalItemPriceStr)
                         .append("</td>")
                         .append("</tr>");
            }
        }

        String subtotalStr = "₹" + (order.getSubtotalAmount() != null ? order.getSubtotalAmount().setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        String deliveryChargeStr = "₹" + (order.getDeliveryCharge() != null ? order.getDeliveryCharge().setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        String taxStr = "₹" + (order.getTaxAmount() != null ? order.getTaxAmount().setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        String totalStr = "₹" + (order.getTotalAmount() != null ? order.getTotalAmount().setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

        StringBuilder discountRowHtml = new StringBuilder();
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            String couponSuffix = (order.getCouponCode() != null && !order.getCouponCode().isBlank()) ? " (" + escapeHtml(order.getCouponCode()) + ")" : "";
            discountRowHtml.append("<tr>")
                           .append("<td align=\"right\" style=\"padding: 6px 0; font-size: 14px; color: #10b981;\">Discount").append(couponSuffix).append(":</td>")
                           .append("<td align=\"right\" style=\"padding: 6px 0; font-size: 14px; color: #10b981; font-weight: 600; width: 120px;\">-₹").append(order.getDiscountAmount().setScale(2, java.math.RoundingMode.HALF_UP)).append("</td>")
                           .append("</tr>");
        }

        // Build address/pickup block
        StringBuilder addressBlockHtml = new StringBuilder();
        if ("PICKUP".equalsIgnoreCase(deliveryType)) {
            Store store = order.getStore();
            String storeName = store != null ? store.getName() : "Main Store";
            String storeAddress = store != null ? store.getAddress() : "";
            String storeCity = store != null ? store.getCity() : "";
            String storePhone = store != null ? store.getPhone() : "";
            String storeTimings = store != null ? store.getTimings() : "";

            addressBlockHtml.append("<h3 style=\"color: #64748b; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin: 24px 0 12px 0; border-bottom: 2px solid #f1f5f9; padding-bottom: 8px;\">Store Pickup Address</h3>")
                            .append("<div style=\"background-color: #f8fafc; border-radius: 8px; padding: 16px; border: 1px solid #f1f5f9; font-size: 14px; line-height: 1.5; color: #475569;\">")
                            .append("<strong style=\"color: #0f172a; display: block; margin-bottom: 4px;\">").append(escapeHtml(storeName)).append("</strong>")
                            .append("<div>").append(escapeHtml(storeAddress)).append("</div>")
                            .append("<div>").append(escapeHtml(storeCity)).append("</div>");
            if (storePhone != null && !storePhone.isBlank()) {
                addressBlockHtml.append("<div style=\"margin-top: 6px;\"><strong>Phone:</strong> ").append(escapeHtml(storePhone)).append("</div>");
            }
            if (storeTimings != null && !storeTimings.isBlank()) {
                addressBlockHtml.append("<div><strong>Timings:</strong> ").append(escapeHtml(storeTimings)).append("</div>");
            }
            addressBlockHtml.append("</div>");
        } else {
            String address = order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "";
            String state = order.getDeliveryState() != null ? order.getDeliveryState() : "";
            String postalCode = order.getDeliveryPostalCode() != null ? order.getDeliveryPostalCode() : "";

            addressBlockHtml.append("<h3 style=\"color: #64748b; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin: 24px 0 12px 0; border-bottom: 2px solid #f1f5f9; padding-bottom: 8px;\">Shipping Address</h3>")
                            .append("<div style=\"background-color: #f8fafc; border-radius: 8px; padding: 16px; border: 1px solid #f1f5f9; font-size: 14px; line-height: 1.5; color: #475569;\">")
                            .append("<strong style=\"color: #0f172a; display: block; margin-bottom: 4px;\">").append(escapeHtml(order.getContactName())).append("</strong>")
                            .append("<div>").append(escapeHtml(address)).append("</div>")
                            .append("<div>").append(escapeHtml(state)).append(" - ").append(escapeHtml(postalCode)).append("</div>")
                            .append("<div style=\"margin-top: 6px;\"><strong>Phone:</strong> ").append(escapeHtml(order.getContactPhone())).append("</div>")
                            .append("</div>");
        }

        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; padding: 20px 0; margin: 0;\">"
                + "<table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05); border: 1px solid #e1e8ed;\">"
                + "<tr>"
                + "<td bgcolor=\"#123b8f\" style=\"padding: 40px 30px; text-align: center; background: linear-gradient(135deg, #123b8f 0%, #1e4bb0 100%);\">"
                + "<h1 style=\"color: #ffffff; margin: 0; font-size: 28px; font-weight: 700; letter-spacing: -0.5px;\">VR Technologies</h1>"
                + "<p style=\"color: #cbd5e1; margin: 8px 0 0 0; font-size: 14px; font-weight: 500;\">Order Update</p>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style=\"padding: 40px 30px;\">"
                + "<h2 style=\"color: #0f172a; margin: 0 0 16px 0; font-size: 20px; font-weight: 700;\">" + subject + "</h2>"
                + "<p style=\"color: #475569; margin: 0 0 24px 0; font-size: 15px; line-height: 1.6;\">Dear " + customerName + ",</p>"
                + "<p style=\"color: #475569; margin: 0 0 24px 0; font-size: 15px; line-height: 1.6;\">" + messageBody + "</p>"
                + "<h3 style=\"color: #64748b; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 12px 0; border-bottom: 2px solid #f1f5f9; padding-bottom: 8px;\">Order Details</h3>"
                + "<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #f8fafc; border-radius: 8px; padding: 16px; margin-bottom: 24px; border: 1px solid #f1f5f9;\">"
                + "<tr>"
                + "<td width=\"50%\" style=\"padding-bottom: 12px;\">"
                + "<span style=\"font-size: 11px; color: #64748b; text-transform: uppercase; display: block; font-weight: 600;\">Order Number</span>"
                + "<span style=\"font-size: 14px; color: #0f172a; font-weight: 700;\">" + orderNumber + "</span>"
                + "</td>"
                + "<td width=\"50%\" style=\"padding-bottom: 12px;\">"
                + "<span style=\"font-size: 11px; color: #64748b; text-transform: uppercase; display: block; font-weight: 600;\">Order Date</span>"
                + "<span style=\"font-size: 14px; color: #0f172a; font-weight: 600;\">" + orderDate + "</span>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td width=\"50%\">"
                + "<span style=\"font-size: 11px; color: #64748b; text-transform: uppercase; display: block; font-weight: 600;\">Fulfillment Method</span>"
                + "<span style=\"font-size: 14px; color: #0f172a; font-weight: 600;\">" + deliveryType + "</span>"
                + "</td>"
                + "<td width=\"50%\">"
                + "<span style=\"font-size: 11px; color: #64748b; text-transform: uppercase; display: block; font-weight: 600;\">Payment Details</span>"
                + "<span style=\"font-size: 14px; color: #0f172a; font-weight: 600;\">" + paymentMethod + " - " + paymentStatus + "</span>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "<h3 style=\"color: #64748b; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 12px 0; border-bottom: 2px solid #f1f5f9; padding-bottom: 8px;\">Items Ordered</h3>"
                + "<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 24px;\">"
                + "<thead>"
                + "<tr>"
                + "<th align=\"left\" style=\"font-size: 12px; color: #64748b; padding-bottom: 10px; border-bottom: 1px solid #e2e8f0; font-weight: 600;\">Item</th>"
                + "<th align=\"center\" style=\"font-size: 12px; color: #64748b; padding-bottom: 10px; border-bottom: 1px solid #e2e8f0; font-weight: 600; width: 60px;\">Qty</th>"
                + "<th align=\"right\" style=\"font-size: 12px; color: #64748b; padding-bottom: 10px; border-bottom: 1px solid #e2e8f0; font-weight: 600; width: 100px;\">Price</th>"
                + "</tr>"
                + "</thead>"
                + "<tbody>"
                + itemsHtml.toString()
                + "</tbody>"
                + "</table>"
                + "<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom: 30px;\">"
                + "<tr>"
                + "<td align=\"right\" style=\"padding: 6px 0; font-size: 14px; color: #475569;\">Subtotal:</td>"
                + "<td align=\"right\" style=\"padding: 6px 0; font-size: 14px; color: #0f172a; font-weight: 600; width: 120px;\">" + subtotalStr + "</td>"
                + "</tr>"
                + discountRowHtml.toString()
                + "<tr>"
                + "<td align=\"right\" style=\"padding: 6px 0; font-size: 14px; color: #475569;\">Delivery Charge:</td>"
                + "<td align=\"right\" style=\"padding: 6px 0; font-size: 14px; color: #0f172a; font-weight: 600;\">" + deliveryChargeStr + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td align=\"right\" style=\"padding: 6px 0; font-size: 14px; color: #475569;\">GST/Tax:</td>"
                + "<td align=\"right\" style=\"padding: 6px 0; font-size: 14px; color: #0f172a; font-weight: 600;\">" + taxStr + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td align=\"right\" style=\"padding: 12px 0 0 0; font-size: 16px; color: #0f172a; font-weight: 700; border-top: 1.5px solid #e2e8f0;\">Total Amount:</td>"
                + "<td align=\"right\" style=\"padding: 12px 0 0 0; font-size: 18px; color: #123b8f; font-weight: 700; border-top: 1.5px solid #e2e8f0;\">" + totalStr + "</td>"
                + "</tr>"
                + "</table>"
                + addressBlockHtml.toString()
                + "<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr>"
                + "<td align=\"center\" style=\"padding-top: 30px;\">"
                + "<a href=\"" + trackingUrl + "\" style=\"background-color: #123b8f; color: #ffffff; padding: 12px 30px; font-weight: 600; text-decoration: none; border-radius: 6px; font-size: 14px; display: inline-block;\">Track Your Order</a>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td bgcolor=\"#f8fafc\" style=\"padding: 30px; text-align: center; border-top: 1px solid #e2e8f0;\">"
                + "<p style=\"color: #64748b; margin: 0; font-size: 13px; font-weight: 500;\">Thank you for shopping with VR Technologies!</p>"
                + "<p style=\"color: #94a3b8; margin: 6px 0 0 0; font-size: 11px;\">If you have any questions, please contact our support team.</p>"
                + "<p style=\"color: #94a3b8; margin: 12px 0 0 0; font-size: 11px;\">&copy; 2026 VR Technologies. All rights reserved.</p>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</div>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
