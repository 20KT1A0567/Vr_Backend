package com.vrtechnologies.vrtech.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrtechnologies.vrtech.dto.request.OrderRequest;
import com.vrtechnologies.vrtech.dto.request.PaymentVerificationRequest;
import com.vrtechnologies.vrtech.dto.response.OrderItemResponse;
import com.vrtechnologies.vrtech.dto.response.OrderResponse;
import com.vrtechnologies.vrtech.dto.response.OrderTimelineEventResponse;
import com.vrtechnologies.vrtech.dto.response.PaymentCheckoutSessionResponse;
import com.vrtechnologies.vrtech.dto.response.PaymentTransactionResponse;
import com.vrtechnologies.vrtech.entity.CartItem;
import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.OrderItem;
import com.vrtechnologies.vrtech.entity.OrderTimelineEvent;
import com.vrtechnologies.vrtech.entity.PaymentTransaction;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.OrderTimelineEventType;
import com.vrtechnologies.vrtech.entity.enums.PaymentGateway;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentTransactionStatus;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.CartItemRepository;
import com.vrtechnologies.vrtech.repository.CustomerOrderRepository;
import com.vrtechnologies.vrtech.repository.PaymentTransactionRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OrderService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final CustomerOrderRepository customerOrderRepository;
    private final CartItemRepository cartItemRepository;
    private final StoreRepository storeRepository;
    private final UserContextService userContextService;
    private final ProductService productService;
    private final PermissionService permissionService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderTimelineService orderTimelineService;
    private final RazorpayService razorpayService;
    private final AdminActivityLogService adminActivityLogService;
    private final ObjectMapper objectMapper;

    public OrderService(
            CustomerOrderRepository customerOrderRepository,
            CartItemRepository cartItemRepository,
            StoreRepository storeRepository,
            UserContextService userContextService,
            ProductService productService,
            PermissionService permissionService,
            PaymentTransactionRepository paymentTransactionRepository,
            OrderTimelineService orderTimelineService,
            RazorpayService razorpayService,
            AdminActivityLogService adminActivityLogService,
            ObjectMapper objectMapper
    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.cartItemRepository = cartItemRepository;
        this.storeRepository = storeRepository;
        this.userContextService = userContextService;
        this.productService = productService;
        this.permissionService = permissionService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderTimelineService = orderTimelineService;
        this.razorpayService = razorpayService;
        this.adminActivityLogService = adminActivityLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        User user = userContextService.getCurrentUser();
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        if (request.getDeliveryType() == null) {
            throw new BadRequestException("Delivery type is required");
        }

        if (request.getDeliveryType().name().equals("DELIVERY")
                && (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank())) {
            throw new BadRequestException("Delivery address is required for delivery orders");
        }

        Store store = storeRepository.findById(request.getStoreId())
                .filter(Store::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setStore(store);
        order.setDeliveryType(request.getDeliveryType());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStatus(OrderStatus.PENDING);
        order.setContactName(trimToNull(request.getContactName()));
        order.setContactPhone(trimToNull(request.getContactPhone()));
        order.setContactEmail(trimToNull(request.getContactEmail()));
        order.setDeliveryAddress(trimToNull(request.getDeliveryAddress()));
        order.setNotes(trimToNull(request.getNotes()));

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            validateCartItemStore(cartItem, store);
            reserveInventory(cartItem.getProduct(), cartItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(cartItem.getProduct());
            item.setQuantity(cartItem.getQuantity());
            item.setPriceAtTime(cartItem.getProduct().getPrice());
            order.getItems().add(item);
            total = total.add(cartItem.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(total);
        CustomerOrder saved = customerOrderRepository.save(order);
        ensureOrderIdentifiers(saved);
        cartItemRepository.deleteByUserId(user.getId());

        orderTimelineService.record(
                saved,
                OrderTimelineEventType.PLACED,
                "Order placed",
                "Customer placed the order and selected " + saved.getDeliveryType().name().toLowerCase(Locale.ROOT) + " fulfilment.",
                user,
                "WEBSITE",
                metadata("paymentMethod", saved.getPaymentMethod().name(), "storeId", saved.getStore() != null ? saved.getStore().getId() : null)
        );

        if (saved.getPaymentMethod() == null || saved.getPaymentMethod().name().equals("CASH")) {
            orderTimelineService.record(
                    saved,
                    OrderTimelineEventType.PAYMENT_PENDING,
                    "Payment pending at store",
                    "This order will be settled offline during pickup or delivery.",
                    user,
                    "WEBSITE"
            );
        }

        return toResponse(saved);
    }

    public List<OrderResponse> getMyOrders() {
        User user = userContextService.getCurrentUser();
        return customerOrderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getMyOrder(Long id) {
        User user = userContextService.getCurrentUser();
        return toResponse(findOwnedOrder(user, id));
    }

    public List<OrderResponse> getAllOrders(User admin) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        return customerOrderRepository.findAll().stream()
                .filter(order -> canAccessOrder(accessibleStoreIds, order))
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getAdminOrder(User admin, Long id) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        requireOrderAccess(admin, order);
        return toResponse(order);
    }

    @Transactional
    public PaymentCheckoutSessionResponse createPaymentCheckout(Long orderId) {
        User user = userContextService.getCurrentUser();
        CustomerOrder order = findOwnedOrder(user, orderId);
        ensureOrderIdentifiers(order);

        if (order.getPaymentMethod() == null || order.getPaymentMethod().name().equals("CASH")) {
            throw new BadRequestException("Cash orders do not require an online payment session");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new BadRequestException("This order cannot accept new payment attempts");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("This order is already paid");
        }

        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("orderId", order.getId());
        notes.put("orderNumber", order.getOrderNumber());
        notes.put("customerEmail", order.getContactEmail());
        notes.put("customerPhone", order.getContactPhone());

        String receipt = truncate(order.getOrderNumber() != null ? order.getOrderNumber() : "ORD-" + order.getId(), 40);
        Map<String, Object> gatewayOrder = razorpayService.createOrder(order.getTotalAmount(), receipt, notes);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setGateway(PaymentGateway.RAZORPAY);
        transaction.setStatus(PaymentTransactionStatus.CREATED);
        transaction.setAmount(order.getTotalAmount());
        transaction.setCurrency(stringValue(gatewayOrder.get("currency"), razorpayService.currency()));
        transaction.setReceipt(receipt);
        transaction.setGatewayOrderId(stringValue(gatewayOrder.get("id"), null));
        transaction.setGatewayStatus(stringValue(gatewayOrder.get("status"), "created"));
        transaction.setMetadata(new LinkedHashMap<>(gatewayOrder));
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);

        orderTimelineService.record(
                order,
                OrderTimelineEventType.PAYMENT_PENDING,
                "Payment initiated",
                "A Razorpay checkout session was created for this order.",
                user,
                "WEBSITE",
                metadata("transactionId", saved.getId(), "gatewayOrderId", saved.getGatewayOrderId())
        );

        return PaymentCheckoutSessionResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .transactionId(saved.getId())
                .gateway(saved.getGateway())
                .keyId(razorpayService.keyId())
                .gatewayOrderId(saved.getGatewayOrderId())
                .amount(saved.getAmount())
                .currency(saved.getCurrency())
                .merchantName(razorpayService.merchantName())
                .description("Payment for order " + order.getOrderNumber())
                .customerName(order.getContactName())
                .customerEmail(order.getContactEmail())
                .customerPhone(order.getContactPhone())
                .build();
    }

    @Transactional
    public OrderResponse verifyPayment(Long orderId, PaymentVerificationRequest request) {
        User user = userContextService.getCurrentUser();
        CustomerOrder order = findOwnedOrder(user, orderId);
        PaymentTransaction transaction = paymentTransactionRepository.findFirstByGatewayOrderIdOrderByCreatedAtDescIdDesc(request.getRazorpayOrderId())
                .filter(item -> item.getOrder().getId().equals(order.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));

        razorpayService.verifyPaymentSignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        transaction.setGatewayPaymentId(request.getRazorpayPaymentId());
        transaction.setGatewaySignature(request.getRazorpaySignature());
        transaction.setGatewayStatus("paid");
        transaction.setStatus(PaymentTransactionStatus.CAPTURED);
        transaction.setVerifiedAt(LocalDateTime.now());
        transaction.setPaidAt(LocalDateTime.now());
        paymentTransactionRepository.save(transaction);

        order.setPaymentStatus(PaymentStatus.PAID);
        if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }
        customerOrderRepository.save(order);

        orderTimelineService.record(
                order,
                OrderTimelineEventType.PAYMENT_CAPTURED,
                "Payment verified",
                "Razorpay signature verification succeeded and the order payment is now marked as paid.",
                user,
                "WEBSITE",
                metadata("transactionId", transaction.getId(), "gatewayPaymentId", request.getRazorpayPaymentId())
        );

        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelMyOrder(Long orderId, String reason) {
        User user = userContextService.getCurrentUser();
        CustomerOrder order = findOwnedOrder(user, orderId);
        if (!canCancel(order)) {
            throw new BadRequestException("This order can no longer be cancelled");
        }
        cancelOrder(order, trimToNull(reason), user, "WEBSITE");
        return toResponse(order);
    }

    @Transactional
    public OrderResponse requestReturn(Long orderId, String reason) {
        User user = userContextService.getCurrentUser();
        CustomerOrder order = findOwnedOrder(user, orderId);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Return requests are allowed only after delivery");
        }
        if (order.getStatus() == OrderStatus.RETURN_REQUESTED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new BadRequestException("A return request already exists for this order");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnRequestedAt(LocalDateTime.now());
        order.setReturnReason(trimToNull(reason));
        customerOrderRepository.save(order);

        orderTimelineService.record(
                order,
                OrderTimelineEventType.RETURN_REQUESTED,
                "Return requested",
                "Customer requested a return for this order.",
                user,
                "WEBSITE",
                metadata("reason", trimToNull(reason))
        );

        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(User admin, Long id, OrderStatus status) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        requireOrderAccess(admin, order);

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus != OrderStatus.CANCELLED && status == OrderStatus.CANCELLED) {
            cancelOrder(order, order.getCancellationReason(), admin, "ADMIN");
        } else {
            if (previousStatus == OrderStatus.CANCELLED && status != OrderStatus.CANCELLED) {
                for (OrderItem item : order.getItems()) {
                    reserveInventory(item.getProduct(), item.getQuantity());
                }
                order.setCancelledAt(null);
                order.setCancellationReason(null);
            }

            order.setStatus(status);
            if (status == OrderStatus.DELIVERED) {
                order.setDeliveredAt(LocalDateTime.now());
            }
            if (status == OrderStatus.RETURN_REQUESTED && order.getReturnRequestedAt() == null) {
                order.setReturnRequestedAt(LocalDateTime.now());
            }
            if (status == OrderStatus.REFUNDED) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
            customerOrderRepository.save(order);

            OrderTimelineEventType eventType = timelineTypeForStatus(status);
            if (eventType != null) {
                orderTimelineService.record(
                        order,
                        eventType,
                        formatStatusTitle(status),
                        "Admin updated the order status from " + previousStatus.name() + " to " + status.name() + ".",
                        admin,
                        "ADMIN"
                );
            }
        }

        adminActivityLogService.log(
                admin,
                Module.ORDERS,
                PermissionAction.UPDATE,
                "ORDER",
                order.getId(),
                previousStatus.name(),
                status.name(),
                "Updated order status for " + safeOrderLabel(order)
        );

        return toResponse(order);
    }

    @Transactional
    public OrderResponse updatePaymentStatus(User admin, Long id, PaymentStatus paymentStatus) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        requireOrderAccess(admin, order);

        PaymentStatus previousStatus = order.getPaymentStatus();
        order.setPaymentStatus(paymentStatus);
        if (paymentStatus == PaymentStatus.PAID && order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
            createManualPaymentTransaction(order, admin, PaymentTransactionStatus.CAPTURED, "Marked paid from admin panel");
            orderTimelineService.record(
                    order,
                    OrderTimelineEventType.PAYMENT_CAPTURED,
                    "Payment marked as paid",
                    "Admin marked this payment as paid.",
                    admin,
                    "ADMIN"
            );
        } else if (paymentStatus == PaymentStatus.FAILED) {
            createManualPaymentTransaction(order, admin, PaymentTransactionStatus.FAILED, "Marked failed from admin panel");
            orderTimelineService.record(
                    order,
                    OrderTimelineEventType.PAYMENT_FAILED,
                    "Payment failed",
                    "Admin marked this payment attempt as failed.",
                    admin,
                    "ADMIN"
            );
        } else if (paymentStatus == PaymentStatus.REFUNDED) {
            order.setStatus(OrderStatus.REFUNDED);
            createManualPaymentTransaction(order, admin, PaymentTransactionStatus.REFUNDED, "Refund recorded from admin panel");
            orderTimelineService.record(
                    order,
                    OrderTimelineEventType.REFUNDED,
                    "Refund recorded",
                    "Admin marked this order as refunded.",
                    admin,
                    "ADMIN"
            );
        }

        customerOrderRepository.save(order);

        adminActivityLogService.log(
                admin,
                Module.ORDERS,
                PermissionAction.UPDATE,
                "ORDER_PAYMENT",
                order.getId(),
                previousStatus.name(),
                paymentStatus.name(),
                "Updated payment status for " + safeOrderLabel(order)
        );

        return toResponse(order);
    }

    public String generateInvoiceHtmlForUser(Long orderId) {
        User user = userContextService.getCurrentUser();
        return generateInvoiceHtml(findOwnedOrder(user, orderId));
    }

    public String generateInvoiceHtmlForAdmin(User admin, Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        requireOrderAccess(admin, order);
        return generateInvoiceHtml(order);
    }

    @Transactional
    public void handleRazorpayWebhook(String payload, String signature, String eventId, String userAgent) {
        razorpayService.verifyWebhookSignature(payload, signature);

        Map<String, Object> envelope;
        try {
            envelope = objectMapper.readValue(payload, MAP_TYPE);
        } catch (Exception ex) {
            throw new BadRequestException("Unable to parse Razorpay webhook payload");
        }

        String event = stringValue(envelope.get("event"), null);
        Map<String, Object> payloadMap = mapValue(envelope.get("payload"));
        Map<String, Object> paymentEntity = nestedEntity(payloadMap, "payment");
        Map<String, Object> orderEntity = nestedEntity(payloadMap, "order");

        String gatewayPaymentId = stringValue(paymentEntity.get("id"), null);
        String gatewayOrderId = stringValue(paymentEntity.get("order_id"), stringValue(orderEntity.get("id"), null));

        if (gatewayPaymentId == null && gatewayOrderId == null) {
            return;
        }

        PaymentTransaction transaction = gatewayPaymentId != null
                ? paymentTransactionRepository.findFirstByGatewayPaymentIdOrderByCreatedAtDescIdDesc(gatewayPaymentId).orElse(null)
                : null;
        if (transaction == null && gatewayOrderId != null) {
            transaction = paymentTransactionRepository.findFirstByGatewayOrderIdOrderByCreatedAtDescIdDesc(gatewayOrderId).orElse(null);
        }
        if (transaction == null) {
            return;
        }

        CustomerOrder order = transaction.getOrder();
        transaction.setGatewayEventId(eventId);
        if (gatewayPaymentId != null) {
            transaction.setGatewayPaymentId(gatewayPaymentId);
        }
        if (gatewayOrderId != null) {
            transaction.setGatewayOrderId(gatewayOrderId);
        }
        transaction.setGatewayStatus(stringValue(paymentEntity.get("status"), stringValue(orderEntity.get("status"), transaction.getGatewayStatus())));
        transaction.setMetadata(mergeMetadata(transaction.getMetadata(), envelope, userAgent));

        if ("payment.authorized".equals(event)) {
            transaction.setStatus(PaymentTransactionStatus.AUTHORIZED);
            paymentTransactionRepository.save(transaction);
            orderTimelineService.record(
                    order,
                    OrderTimelineEventType.PAYMENT_AUTHORIZED,
                    "Payment authorized",
                    "Razorpay authorized the payment for this order.",
                    null,
                    "WEBHOOK",
                    metadata("gatewayPaymentId", gatewayPaymentId)
            );
            return;
        }

        if ("payment.captured".equals(event)) {
            transaction.setStatus(PaymentTransactionStatus.CAPTURED);
            transaction.setPaidAt(LocalDateTime.now());
            transaction.setVerifiedAt(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);

            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getPaidAt() == null) {
                order.setPaidAt(LocalDateTime.now());
            }
            customerOrderRepository.save(order);

            orderTimelineService.record(
                    order,
                    OrderTimelineEventType.PAYMENT_CAPTURED,
                    "Payment captured",
                    "Razorpay webhook confirmed that the payment was captured.",
                    null,
                    "WEBHOOK",
                    metadata("gatewayPaymentId", gatewayPaymentId)
            );
            return;
        }

        if ("payment.failed".equals(event)) {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setFailureReason(extractFailureReason(paymentEntity));
            paymentTransactionRepository.save(transaction);

            order.setPaymentStatus(PaymentStatus.FAILED);
            customerOrderRepository.save(order);

            orderTimelineService.record(
                    order,
                    OrderTimelineEventType.PAYMENT_FAILED,
                    "Payment failed",
                    "Razorpay reported a failed payment attempt.",
                    null,
                    "WEBHOOK",
                    metadata("gatewayPaymentId", gatewayPaymentId, "reason", transaction.getFailureReason())
            );
            return;
        }

        if ("refund.processed".equals(event) || "payment.refunded".equals(event)) {
            transaction.setStatus(PaymentTransactionStatus.REFUNDED);
            transaction.setRefundedAt(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);

            order.setPaymentStatus(PaymentStatus.REFUNDED);
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.RETURN_REQUESTED) {
                order.setStatus(OrderStatus.REFUNDED);
            }
            customerOrderRepository.save(order);

            orderTimelineService.record(
                    order,
                    OrderTimelineEventType.REFUNDED,
                    "Refund processed",
                    "Razorpay webhook confirmed that the payment was refunded.",
                    null,
                    "WEBHOOK",
                    metadata("gatewayPaymentId", gatewayPaymentId)
            );
        }
    }

    private OrderResponse toResponse(CustomerOrder order) {
        ensureOrderIdentifiers(order);
        List<OrderTimelineEventResponse> timeline = orderTimelineService.findForOrder(order.getId()).stream()
                .map(this::toTimelineResponse)
                .toList();
        PaymentTransactionResponse latestPayment = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDescIdDesc(order.getId())
                .map(this::toPaymentResponse)
                .orElse(null);

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .invoiceNumber(order.getInvoiceNumber())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .deliveryType(order.getDeliveryType())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .store(order.getStore() != null ? productService.toStoreSummaryResponse(order.getStore()) : null)
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .contactEmail(order.getContactEmail())
                .deliveryAddress(order.getDeliveryAddress())
                .notes(order.getNotes())
                .cancellationReason(order.getCancellationReason())
                .returnReason(order.getReturnReason())
                .paidAt(order.getPaidAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .returnRequestedAt(order.getReturnRequestedAt())
                .latestPayment(latestPayment)
                .timeline(timeline)
                .items(order.getItems().stream().map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .quantity(item.getQuantity())
                        .priceAtTime(item.getPriceAtTime())
                        .product(productService.toProductResponse(item.getProduct()))
                        .build()).toList())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private PaymentTransactionResponse toPaymentResponse(PaymentTransaction transaction) {
        return PaymentTransactionResponse.builder()
                .id(transaction.getId())
                .gateway(transaction.getGateway())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .receipt(transaction.getReceipt())
                .gatewayOrderId(transaction.getGatewayOrderId())
                .gatewayPaymentId(transaction.getGatewayPaymentId())
                .gatewayStatus(transaction.getGatewayStatus())
                .failureReason(transaction.getFailureReason())
                .verifiedAt(transaction.getVerifiedAt())
                .paidAt(transaction.getPaidAt())
                .refundedAt(transaction.getRefundedAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private OrderTimelineEventResponse toTimelineResponse(OrderTimelineEvent event) {
        return OrderTimelineEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .title(event.getTitle())
                .description(event.getDescription())
                .source(event.getSource())
                .actorId(event.getActorId())
                .actorName(event.getActorName())
                .actorEmail(event.getActorEmail())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private void validateCartItemStore(CartItem cartItem, Store store) {
        boolean availableInStore = cartItem.getProduct().getStores().stream()
                .anyMatch(mappedStore -> mappedStore.getId().equals(store.getId()) && mappedStore.isActive());
        if (!availableInStore) {
            throw new BadRequestException(cartItem.getProduct().getTitle() + " is not mapped to the selected store");
        }
    }

    private void reserveInventory(Product product, int quantity) {
        int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        if (!product.isAvailable() || currentStock < quantity) {
            throw new BadRequestException(product.getTitle() + " does not have enough stock");
        }
        product.setStockQuantity(currentStock - quantity);
        if (product.getStockQuantity() <= 0) {
            product.setAvailable(false);
        }
    }

    private void releaseInventory(Product product, int quantity) {
        int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        product.setStockQuantity(currentStock + quantity);
        if (product.getStockQuantity() > 0) {
            product.setAvailable(true);
        }
    }

    private void requireOrderAccess(User admin, CustomerOrder order) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        if (!canAccessOrder(accessibleStoreIds, order)) {
            throw new AccessDeniedException("You do not have access to this order");
        }
    }

    private boolean canAccessOrder(List<Long> accessibleStoreIds, CustomerOrder order) {
        if (accessibleStoreIds == null || accessibleStoreIds.isEmpty()) {
            return true;
        }
        return order.getStore() != null && accessibleStoreIds.contains(order.getStore().getId());
    }

    private CustomerOrder findOwnedOrder(User user, Long id) {
        return customerOrderRepository.findById(id)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private void ensureOrderIdentifiers(CustomerOrder order) {
        boolean changed = false;
        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            order.setOrderNumber(String.format("VRT-ORD-%06d", order.getId()));
            changed = true;
        }
        if (order.getInvoiceNumber() == null || order.getInvoiceNumber().isBlank()) {
            order.setInvoiceNumber(String.format("VRT-INV-%06d", order.getId()));
            changed = true;
        }
        if (changed) {
            customerOrderRepository.save(order);
        }
    }

    private boolean canCancel(CustomerOrder order) {
        return switch (order.getStatus()) {
            case PENDING, CONFIRMED, PACKED -> true;
            default -> false;
        };
    }

    private void cancelOrder(CustomerOrder order, String reason, User actor, String source) {
        if (order.getStatus() != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                releaseInventory(item.getProduct(), item.getQuantity());
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);
        customerOrderRepository.save(order);

        orderTimelineService.record(
                order,
                OrderTimelineEventType.CANCELLED,
                "Order cancelled",
                source.equals("ADMIN") ? "Admin cancelled this order." : "Customer cancelled this order.",
                actor,
                source,
                metadata("reason", reason)
        );
    }

    private void createManualPaymentTransaction(CustomerOrder order, User admin, PaymentTransactionStatus status, String description) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setGateway(PaymentGateway.OFFLINE);
        transaction.setStatus(status);
        transaction.setAmount(order.getTotalAmount());
        transaction.setCurrency("INR");
        transaction.setReceipt(order.getOrderNumber());
        transaction.setGatewayStatus(status.name().toLowerCase(Locale.ROOT));
        if (status == PaymentTransactionStatus.CAPTURED) {
            transaction.setPaidAt(LocalDateTime.now());
            transaction.setVerifiedAt(LocalDateTime.now());
        }
        if (status == PaymentTransactionStatus.REFUNDED) {
            transaction.setRefundedAt(LocalDateTime.now());
        }
        transaction.setMetadata(metadata("description", description, "actorEmail", admin.getEmail()));
        paymentTransactionRepository.save(transaction);
    }

    private OrderTimelineEventType timelineTypeForStatus(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> OrderTimelineEventType.CONFIRMED;
            case PACKED -> OrderTimelineEventType.PACKED;
            case SHIPPED -> OrderTimelineEventType.SHIPPED;
            case READY -> OrderTimelineEventType.READY;
            case DELIVERED -> OrderTimelineEventType.DELIVERED;
            case CANCELLED -> OrderTimelineEventType.CANCELLED;
            case RETURN_REQUESTED -> OrderTimelineEventType.RETURN_REQUESTED;
            case REFUNDED -> OrderTimelineEventType.REFUNDED;
            default -> null;
        };
    }

    private String formatStatusTitle(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Order pending";
            case CONFIRMED -> "Order confirmed";
            case PACKED -> "Order packed";
            case SHIPPED -> "Order shipped";
            case READY -> "Order ready";
            case DELIVERED -> "Order delivered";
            case CANCELLED -> "Order cancelled";
            case RETURN_REQUESTED -> "Return requested";
            case REFUNDED -> "Order refunded";
        };
    }

    private String safeOrderLabel(CustomerOrder order) {
        return order.getOrderNumber() != null ? order.getOrderNumber() : "order #" + order.getId();
    }

    private String generateInvoiceHtml(CustomerOrder order) {
        ensureOrderIdentifiers(order);

        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemsHtml.append("""
                    <tr>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(item.getProduct().getTitle()),
                    item.getQuantity(),
                    formatCurrency(item.getPriceAtTime()),
                    formatCurrency(lineTotal)
            ));
        }

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>%s</title>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 0; background: #f5f7fb; color: #0f172a; }
                    .wrap { max-width: 920px; margin: 32px auto; background: #ffffff; border-radius: 24px; padding: 32px; box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08); }
                    .header { display: flex; justify-content: space-between; gap: 24px; flex-wrap: wrap; }
                    .badge { display: inline-block; padding: 8px 14px; border-radius: 999px; background: #e8f1ff; color: #1e3a8a; font-size: 12px; font-weight: 700; letter-spacing: 0.16em; text-transform: uppercase; }
                    h1 { margin: 16px 0 8px; font-size: 34px; }
                    h2 { font-size: 18px; margin: 0 0 12px; }
                    .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; margin-top: 28px; }
                    .card { border: 1px solid rgba(30, 58, 138, 0.08); background: #f8fbff; border-radius: 18px; padding: 18px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 28px; }
                    th, td { padding: 14px 12px; border-bottom: 1px solid #e2e8f0; text-align: left; }
                    th { font-size: 12px; text-transform: uppercase; letter-spacing: 0.14em; color: #64748b; }
                    .total { margin-top: 28px; display: flex; justify-content: flex-end; font-size: 24px; font-weight: 700; }
                    .footer { margin-top: 32px; font-size: 13px; color: #64748b; }
                    @media print { body { background: white; } .wrap { box-shadow: none; margin: 0; max-width: none; border-radius: 0; } }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="header">
                      <div>
                        <div class="badge">Invoice</div>
                        <h1>%s</h1>
                        <div>Order %s</div>
                        <div>Issued %s</div>
                      </div>
                      <div>
                        <h2>VR Technologies</h2>
                        <div>Refurbished systems marketplace</div>
                        <div>%s</div>
                        <div>%s</div>
                      </div>
                    </div>

                    <div class="grid">
                      <div class="card">
                        <h2>Bill To</h2>
                        <div>%s</div>
                        <div>%s</div>
                        <div>%s</div>
                        <div>%s</div>
                      </div>
                      <div class="card">
                        <h2>Fulfilment</h2>
                        <div>Status: %s</div>
                        <div>Payment: %s</div>
                        <div>Method: %s</div>
                        <div>Store: %s</div>
                      </div>
                    </div>

                    <table>
                      <thead>
                        <tr>
                          <th>Product</th>
                          <th>Qty</th>
                          <th>Unit Price</th>
                          <th>Line Total</th>
                        </tr>
                      </thead>
                      <tbody>
                        %s
                      </tbody>
                    </table>

                    <div class="total">Grand Total: %s</div>

                    <div class="footer">
                      Thank you for shopping with VR Technologies. This invoice was generated digitally and is valid without a signature.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(order.getInvoiceNumber()),
                escapeHtml(order.getInvoiceNumber()),
                escapeHtml(order.getOrderNumber()),
                DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").format(order.getCreatedAt()),
                order.getStore() != null ? escapeHtml(order.getStore().getName()) : "Online order",
                order.getStore() != null ? escapeHtml(order.getStore().getAddress() + ", " + order.getStore().getCity()) : "Website checkout",
                escapeHtml(defaultString(order.getContactName(), "Customer")),
                escapeHtml(defaultString(order.getContactPhone(), "-")),
                escapeHtml(defaultString(order.getContactEmail(), "-")),
                escapeHtml(defaultString(order.getDeliveryType() != null && order.getDeliveryType().name().equals("DELIVERY") ? order.getDeliveryAddress() : order.getStore() != null ? order.getStore().getAddress() : "-", "-")),
                escapeHtml(order.getStatus().name()),
                escapeHtml(order.getPaymentStatus().name()),
                escapeHtml(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "-"),
                escapeHtml(order.getStore() != null ? order.getStore().getName() : "Not assigned"),
                itemsHtml,
                formatCurrency(order.getTotalAmount())
        );
    }

    private Map<String, Object> nestedEntity(Map<String, Object> payloadMap, String key) {
        Map<String, Object> wrapper = mapValue(payloadMap.get(key));
        return mapValue(wrapper.get("entity"));
    }

    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            rawMap.forEach((key, item) -> normalized.put(String.valueOf(key), item));
            return normalized;
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> mergeMetadata(Map<String, Object> current, Map<String, Object> payload, String userAgent) {
        Map<String, Object> merged = current == null ? new LinkedHashMap<>() : new LinkedHashMap<>(current);
        merged.put("lastWebhookPayload", payload);
        merged.put("lastWebhookUserAgent", userAgent);
        return merged;
    }

    private String extractFailureReason(Map<String, Object> paymentEntity) {
        String description = stringValue(paymentEntity.get("error_description"), null);
        if (description != null) {
            return description;
        }
        String reason = stringValue(paymentEntity.get("error_reason"), null);
        return reason != null ? reason : "Payment failed";
    }

    private String escapeHtml(String value) {
        return defaultString(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String formatCurrency(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return "Rs. " + safeValue.toPlainString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String result = String.valueOf(value).trim();
        return result.isEmpty() ? fallback : result;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Map<String, Object> metadata(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            values.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return values;
    }
}
