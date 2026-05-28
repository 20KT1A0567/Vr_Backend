package com.vrtechnologies.vrtech.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrtechnologies.vrtech.dto.request.OrderRequest;
import com.vrtechnologies.vrtech.dto.request.PaymentVerificationRequest;
import com.vrtechnologies.vrtech.dto.request.ShipmentUpdateRequest;
import com.vrtechnologies.vrtech.dto.request.CourierWebhookRequest;
import com.vrtechnologies.vrtech.dto.response.CheckoutProfileResponse;
import com.vrtechnologies.vrtech.dto.response.OrderItemResponse;
import com.vrtechnologies.vrtech.dto.response.OrderResponse;
import com.vrtechnologies.vrtech.dto.response.OrderTimelineEventResponse;
import com.vrtechnologies.vrtech.dto.response.PaymentCheckoutSessionResponse;
import com.vrtechnologies.vrtech.dto.response.PaymentRecoveryResponse;
import com.vrtechnologies.vrtech.dto.response.PaymentTransactionResponse;
import com.vrtechnologies.vrtech.dto.response.PaymentWebhookEventResponse;
import com.vrtechnologies.vrtech.dto.response.RefundTransactionResponse;
import com.vrtechnologies.vrtech.dto.response.CouponValidationResponse;
import com.vrtechnologies.vrtech.entity.CartItem;
import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.OrderItem;
import com.vrtechnologies.vrtech.entity.OrderTimelineEvent;
import com.vrtechnologies.vrtech.entity.PaymentTransaction;
import com.vrtechnologies.vrtech.entity.PaymentWebhookEvent;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.RefundTransaction;
import com.vrtechnologies.vrtech.entity.SiteSettings;
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
import com.vrtechnologies.vrtech.repository.PaymentWebhookEventRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.ProductStoreStockRepository;
import com.vrtechnologies.vrtech.repository.RefundTransactionRepository;
import com.vrtechnologies.vrtech.repository.SiteSettingsRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import com.vrtechnologies.vrtech.repository.UserAddressRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class OrderService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final CustomerOrderRepository customerOrderRepository;
    private final CartItemRepository cartItemRepository;
    private final StoreRepository storeRepository;
    private final UserContextService userContextService;
    private final ProductService productService;
    private final PermissionService permissionService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final ProductRepository productRepository;
    private final ProductStoreStockRepository productStoreStockRepository;
    private final OrderTimelineService orderTimelineService;
    private final RazorpayService razorpayService;
    private final AdminActivityLogService adminActivityLogService;
    private final CouponService couponService;
    private final NotificationService notificationService;
    private final UserAddressRepository userAddressRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final PincodeDeliveryService pincodeDeliveryService;
    private final ObjectMapper objectMapper;

    public OrderService(
            CustomerOrderRepository customerOrderRepository,
            CartItemRepository cartItemRepository,
            StoreRepository storeRepository,
            UserContextService userContextService,
            ProductService productService,
            PermissionService permissionService,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentWebhookEventRepository paymentWebhookEventRepository,
            RefundTransactionRepository refundTransactionRepository,
            ProductRepository productRepository,
            ProductStoreStockRepository productStoreStockRepository,
            OrderTimelineService orderTimelineService,
            RazorpayService razorpayService,
            AdminActivityLogService adminActivityLogService,
            CouponService couponService,
            NotificationService notificationService,
            UserAddressRepository userAddressRepository,
            SiteSettingsRepository siteSettingsRepository,
            PincodeDeliveryService pincodeDeliveryService,
            ObjectMapper objectMapper
    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.cartItemRepository = cartItemRepository;
        this.storeRepository = storeRepository;
        this.userContextService = userContextService;
        this.productService = productService;
        this.permissionService = permissionService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentWebhookEventRepository = paymentWebhookEventRepository;
        this.refundTransactionRepository = refundTransactionRepository;
        this.productRepository = productRepository;
        this.productStoreStockRepository = productStoreStockRepository;
        this.orderTimelineService = orderTimelineService;
        this.razorpayService = razorpayService;
        this.adminActivityLogService = adminActivityLogService;
        this.couponService = couponService;
        this.notificationService = notificationService;
        this.userAddressRepository = userAddressRepository;
        this.siteSettingsRepository = siteSettingsRepository;
        this.pincodeDeliveryService = pincodeDeliveryService;
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
        SiteSettings settings = siteSettingsRepository.findTopByOrderByIdAsc().orElseGet(SiteSettings::new);
        if (request.getDeliveryType().name().equals("PICKUP") && !settings.isPickupEnabled()) {
            throw new BadRequestException("Store pickup is currently disabled");
        }
        if (request.getDeliveryType().name().equals("DELIVERY") && !settings.isDeliveryEnabled()) {
            throw new BadRequestException("Delivery is currently disabled");
        }

        if (request.getDeliveryType().name().equals("DELIVERY")
                && (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank())) {
            throw new BadRequestException("Delivery address is required for delivery orders");
        }
        if (request.getDeliveryType().name().equals("DELIVERY")
                && (request.getDeliveryState() == null || request.getDeliveryState().isBlank())) {
            throw new BadRequestException("Delivery state is required for delivery orders");
        }
        if (request.getDeliveryType().name().equals("DELIVERY")
                && (request.getDeliveryPostalCode() == null || request.getDeliveryPostalCode().isBlank())) {
            throw new BadRequestException("Delivery pincode is required for delivery orders");
        }

        if (isOnlinePaymentMethod(request.getPaymentMethod()) && !razorpayService.isEnabled()) {
            throw new BadRequestException("Razorpay payments are not configured yet");
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
        order.setContactEmail(sanitizeContactEmail(request.getContactEmail()));
        order.setDeliveryAddress(trimToNull(request.getDeliveryAddress()));
        order.setDeliveryState(trimToNull(request.getDeliveryState()));
        order.setDeliveryPostalCode(trimToNull(request.getDeliveryPostalCode()));
        order.setNotes(trimToNull(request.getNotes()));
        updateUserCheckoutPreferences(user, request);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            validateCartItemStore(cartItem, store);
            reserveInventory(cartItem.getProduct(), store, cartItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(cartItem.getProduct());
            item.setQuantity(cartItem.getQuantity());
            item.setPriceAtTime(cartItem.getProduct().getPrice());
            order.getItems().add(item);
            total = total.add(cartItem.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = trimToNull(request.getCouponCode());
        if (couponCode != null) {
            CouponValidationResponse validation = couponService.apply(couponCode, total);
            discount = validation.getDiscountAmount();
            order.setCouponCode(validation.getCode());
        }
        order.setSubtotalAmount(total);
        order.setDiscountAmount(discount);
        BigDecimal afterDiscount = total.subtract(discount).max(BigDecimal.ZERO);
        PincodeDeliveryService.DeliveryResolution deliveryResolution = resolveDelivery(settings, request, store, afterDiscount);
        BigDecimal deliveryCharge = deliveryResolution.getDeliveryCharge();
        BigDecimal taxAmount = calculateTax(settings, afterDiscount);
        order.setDeliveryCharge(deliveryCharge);
        order.setPromisedMinDeliveryDays(deliveryResolution.getMinDeliveryDays());
        order.setPromisedMaxDeliveryDays(deliveryResolution.getMaxDeliveryDays());
        order.setDeliveryRuleId(deliveryResolution.getRuleId());
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(afterDiscount.add(taxAmount).add(deliveryCharge).setScale(2, RoundingMode.HALF_UP));
        CustomerOrder saved = customerOrderRepository.save(order);
        ensureOrderIdentifiers(saved);
        cartItemRepository.deleteByUserId(user.getId());
        if (!isOnlinePaymentMethod(saved.getPaymentMethod())) {
            notificationService.logOrderEvent("ORDER_PLACED", saved, "Order placed", "Your order " + saved.getOrderNumber() + " has been placed.");
        }

        orderTimelineService.record(
                saved,
                OrderTimelineEventType.PLACED,
                "Order placed",
                "Customer placed the order and selected " + saved.getDeliveryType().name().toLowerCase(Locale.ROOT) + " fulfilment.",
                user,
                "WEBSITE",
                metadata("paymentMethod", saved.getPaymentMethod().name(), "storeId", saved.getStore() != null ? saved.getStore().getId() : null)
        );

        if (!isOnlinePaymentMethod(saved.getPaymentMethod())) {
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

    @Transactional
    public OrderResponse placeGuestOrder(OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("At least one item is required for guest checkout");
        }
        if (isOnlinePaymentMethod(request.getPaymentMethod()) && !razorpayService.isEnabled()) {
            throw new BadRequestException("Razorpay payments are not configured yet");
        }
        SiteSettings settings = siteSettingsRepository.findTopByOrderByIdAsc().orElseGet(SiteSettings::new);
        if (request.getDeliveryType() == null) {
            throw new BadRequestException("Delivery type is required");
        }
        if (request.getDeliveryType().name().equals("PICKUP") && !settings.isPickupEnabled()) {
            throw new BadRequestException("Store pickup is currently disabled");
        }
        if (request.getDeliveryType().name().equals("DELIVERY") && !settings.isDeliveryEnabled()) {
            throw new BadRequestException("Delivery is currently disabled");
        }
        if (request.getDeliveryType().name().equals("DELIVERY")
                && (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank())) {
            throw new BadRequestException("Delivery address is required for delivery orders");
        }
        if (request.getDeliveryType().name().equals("DELIVERY")
                && (request.getDeliveryState() == null || request.getDeliveryState().isBlank())) {
            throw new BadRequestException("Delivery state is required for delivery orders");
        }
        if (request.getDeliveryType().name().equals("DELIVERY")
                && (request.getDeliveryPostalCode() == null || request.getDeliveryPostalCode().isBlank())) {
            throw new BadRequestException("Delivery pincode is required for delivery orders");
        }
        Store store = storeRepository.findById(request.getStoreId())
                .filter(Store::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        CustomerOrder order = new CustomerOrder();
        order.setGuestCheckout(true);
        order.setStore(store);
        order.setDeliveryType(request.getDeliveryType());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStatus(OrderStatus.PENDING);
        order.setContactName(trimToNull(request.getContactName()));
        order.setContactPhone(trimToNull(request.getContactPhone()));
        order.setContactEmail(sanitizeContactEmail(request.getContactEmail()));
        order.setDeliveryAddress(trimToNull(request.getDeliveryAddress()));
        order.setDeliveryState(trimToNull(request.getDeliveryState()));
        order.setDeliveryPostalCode(trimToNull(request.getDeliveryPostalCode()));
        order.setNotes(trimToNull(request.getNotes()));

        BigDecimal total = BigDecimal.ZERO;
        for (var requestedItem : request.getItems()) {
            Product product = productRepository.findById(requestedItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            validateProductStore(product, store);
            reserveInventory(product, store, requestedItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(requestedItem.getQuantity());
            item.setPriceAtTime(product.getPrice());
            order.getItems().add(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(requestedItem.getQuantity())));
        }

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = trimToNull(request.getCouponCode());
        if (couponCode != null) {
            CouponValidationResponse validation = couponService.apply(couponCode, total);
            discount = validation.getDiscountAmount();
            order.setCouponCode(validation.getCode());
        }
        order.setSubtotalAmount(total);
        order.setDiscountAmount(discount);
        BigDecimal afterDiscount = total.subtract(discount).max(BigDecimal.ZERO);
        PincodeDeliveryService.DeliveryResolution deliveryResolution = resolveDelivery(settings, request, store, afterDiscount);
        order.setDeliveryCharge(deliveryResolution.getDeliveryCharge());
        order.setPromisedMinDeliveryDays(deliveryResolution.getMinDeliveryDays());
        order.setPromisedMaxDeliveryDays(deliveryResolution.getMaxDeliveryDays());
        order.setDeliveryRuleId(deliveryResolution.getRuleId());
        order.setTaxAmount(calculateTax(settings, afterDiscount));
        order.setTotalAmount(afterDiscount.add(order.getTaxAmount()).add(order.getDeliveryCharge()).setScale(2, RoundingMode.HALF_UP));

        CustomerOrder saved = customerOrderRepository.save(order);
        ensureOrderIdentifiers(saved);
        if (!isOnlinePaymentMethod(saved.getPaymentMethod())) {
            notificationService.logOrderEvent("GUEST_ORDER_PLACED", saved, "Guest order placed", "Your order " + saved.getOrderNumber() + " has been placed.");
        }
        orderTimelineService.record(saved, OrderTimelineEventType.PLACED, "Guest order placed", "Guest checkout order was created.", null, "WEBSITE");
        return toResponse(saved);
    }

    public List<OrderResponse> getMyOrders() {
        User user = userContextService.getCurrentUser();
        return customerOrderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public CheckoutProfileResponse getCheckoutProfile() {
        User user = userContextService.getCurrentUser();
        List<CustomerOrder> recentOrders = customerOrderRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());
        List<CheckoutProfileResponse.SavedAddressResponse> savedAddresses = buildSavedAddresses(user, recentOrders);

        return CheckoutProfileResponse.builder()
                .contactName(defaultString(trimToNull(user.getPreferredContactName()), trimToNull(user.getName())))
                .contactPhone(defaultString(trimToNull(user.getPreferredContactPhone()), trimToNull(user.getPhone())))
                .contactEmail(defaultString(sanitizeContactEmail(user.getPreferredContactEmail()), sanitizeContactEmail(user.getEmail())))
                .defaultDeliveryAddress(trimToNull(user.getPreferredDeliveryAddress()))
                .savedAddresses(savedAddresses)
                .build();
    }

    public OrderResponse getMyOrder(Long id) {
        User user = userContextService.getCurrentUser();
        return toResponse(findOwnedOrder(user, id));
    }

    public OrderResponse trackPublicOrder(String orderNumber, String phone) {
        String normalizedOrderNumber = trimToNull(orderNumber);
        String normalizedPhone = digitsOnly(phone);
        if (normalizedOrderNumber == null || normalizedPhone == null || normalizedPhone.length() < 6) {
            throw new BadRequestException("Enter a valid order number and phone number");
        }

        CustomerOrder order = customerOrderRepository.findByOrderNumberIgnoreCase(normalizedOrderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        String orderPhone = digitsOnly(order.getContactPhone());
        String phoneTail = normalizedPhone.substring(Math.max(0, normalizedPhone.length() - 10));
        if (orderPhone == null || !orderPhone.endsWith(phoneTail)) {
            throw new ResourceNotFoundException("Order not found for this phone number");
        }

        return toResponse(order);
    }

    public List<OrderResponse> getAllOrders(User admin) {
        return getAllOrders(admin, null, null);
    }

    public List<OrderResponse> getAllOrders(User admin, LocalDateTime startDate, LocalDateTime endDate) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        return customerOrderRepository.findAll().stream()
                .filter(order -> canAccessOrder(accessibleStoreIds, order))
                .filter(order -> withinDateRange(order.getCreatedAt(), startDate, endDate))
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> getFailedPaymentOrders(User admin) {
        return getFailedPaymentOrders(admin, null, null);
    }

    public List<OrderResponse> getFailedPaymentOrders(User admin, LocalDateTime startDate, LocalDateTime endDate) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        return customerOrderRepository.findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus.FAILED).stream()
                .filter(order -> canAccessOrder(accessibleStoreIds, order))
                .filter(order -> withinDateRange(order.getCreatedAt(), startDate, endDate))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentRecoveryResponse recoverFailedPayment(User admin, Long id) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        requireOrderAccess(admin, order);
        if (order.getPaymentStatus() != PaymentStatus.FAILED && order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Only failed or pending online payments can be recovered");
        }
        if (order.getPaymentMethod() == null || "CASH".equals(order.getPaymentMethod().name())) {
            throw new BadRequestException("Cash orders do not need payment recovery");
        }
        String subject = "Retry payment for order " + safeOrderLabel(order);
        String message = "Payment recovery reminder queued for " + safeOrderLabel(order)
                + ". Customer can open their order and use Retry Payment.";
        var emailLog = notificationService.log("PAYMENT_RECOVERY", "EMAIL", order.getContactEmail(), subject, message, order.getId());
        var whatsappLog = notificationService.log("PAYMENT_RECOVERY", "WHATSAPP", order.getContactPhone(), subject, message, order.getId());
        orderTimelineService.record(
                order,
                OrderTimelineEventType.PAYMENT_FAILED,
                "Payment recovery queued",
                "Admin queued a payment retry reminder.",
                admin,
                "ADMIN",
                metadata("emailStatus", emailLog != null ? emailLog.getStatus() : "SKIPPED", "whatsappStatus", whatsappLog != null ? whatsappLog.getStatus() : "SKIPPED")
        );
        return PaymentRecoveryResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .emailStatus(emailLog != null ? emailLog.getStatus() : "SKIPPED")
                .whatsappStatus(whatsappLog != null ? whatsappLog.getStatus() : "SKIPPED")
                .message("Payment recovery queued for " + safeOrderLabel(order))
                .build();
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

        if (!isOnlinePaymentMethod(order.getPaymentMethod())) {
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
        notes.put("customerEmail", sanitizeContactEmail(order.getContactEmail()));
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
                .customerEmail(sanitizeContactEmail(order.getContactEmail()))
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
        boolean orderConfirmed = promoteToConfirmedIfPending(order);
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
        if (orderConfirmed) {
            orderTimelineService.record(
                    order,
                    OrderTimelineEventType.CONFIRMED,
                    "Order confirmed",
                    "Payment verification completed and the order is now confirmed.",
                    user,
                    "WEBSITE"
            );
        }
        notificationService.logOrderEvent("PAYMENT_SUCCESS", order, "Payment successful", "Payment for " + order.getOrderNumber() + " has been verified.");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse markPaymentFailed(Long orderId, String reason) {
        User user = userContextService.getCurrentUser();
        CustomerOrder order = findOwnedOrder(user, orderId);
        if (!isOnlinePaymentMethod(order.getPaymentMethod())) {
            throw new BadRequestException("Cash orders do not have online payment attempts");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("This order is already paid");
        }

        String failureReason = defaultString(trimToNull(reason), "Payment was not completed");
        PaymentTransaction transaction = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDescIdDesc(order.getId()).orElse(null);
        if (transaction != null && transaction.getStatus() != PaymentTransactionStatus.CAPTURED) {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setFailureReason(failureReason);
            paymentTransactionRepository.save(transaction);
        }

        order.setPaymentStatus(PaymentStatus.FAILED);
        customerOrderRepository.save(order);
        notificationService.logOrderEvent("PAYMENT_FAILED", order, "Payment failed", "Payment failed for " + order.getOrderNumber() + ".");

        orderTimelineService.record(
                order,
                OrderTimelineEventType.PAYMENT_FAILED,
                "Payment failed",
                failureReason,
                user,
                "WEBSITE",
                metadata("transactionId", transaction != null ? transaction.getId() : null)
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
                    reserveInventory(item.getProduct(), order.getStore(), item.getQuantity());
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
            promoteToConfirmedIfPending(order);
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

    @Transactional
    public OrderResponse updateShipment(User admin, Long id, ShipmentUpdateRequest request) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        requireOrderAccess(admin, order);

        String beforeSummary = shipmentSummary(order);
        OrderStatus previousStatus = order.getStatus();

        boolean clear = Boolean.TRUE.equals(request.getClear());
        if (clear) {
            order.setCourierName(null);
            order.setTrackingNumber(null);
            order.setTrackingUrl(null);
            order.setShippedAt(null);
        } else {
            if (request.getCourierName() != null) {
                order.setCourierName(blankToNull(request.getCourierName()));
            }
            if (request.getTrackingNumber() != null) {
                order.setTrackingNumber(blankToNull(request.getTrackingNumber()));
            }
            if (request.getTrackingUrl() != null) {
                order.setTrackingUrl(blankToNull(request.getTrackingUrl()));
            }
        }

        boolean shouldBumpToShipped = !clear
                && Boolean.TRUE.equals(request.getMarkShipped())
                && previousStatus != OrderStatus.SHIPPED
                && previousStatus != OrderStatus.DELIVERED
                && previousStatus != OrderStatus.CANCELLED
                && previousStatus != OrderStatus.REFUNDED;

        if (shouldBumpToShipped) {
            order.setStatus(OrderStatus.SHIPPED);
            if (order.getShippedAt() == null) {
                order.setShippedAt(LocalDateTime.now());
            }
        } else if (!clear && order.getShippedAt() == null
                && (order.getCourierName() != null || order.getTrackingNumber() != null)) {
            // Capture first-shipment timestamp the moment any tracking info is added.
            order.setShippedAt(LocalDateTime.now());
        }

        customerOrderRepository.save(order);

        String afterSummary = shipmentSummary(order);

        if (shouldBumpToShipped) {
            orderTimelineService.record(
                    order,
                    OrderTimelineEventType.SHIPPED,
                    "Order shipped",
                    "Admin marked the order as shipped"
                            + (order.getCourierName() != null ? " via " + order.getCourierName() : "")
                            + (order.getTrackingNumber() != null ? " (AWB " + order.getTrackingNumber() + ")" : "")
                            + ".",
                    admin,
                    "ADMIN"
            );
        }
        // Non-shipping shipment edits are tracked via the admin activity log only,
        // to avoid noisy timeline entries for minor tracking-URL fixes.

        adminActivityLogService.log(
                admin,
                Module.ORDERS,
                PermissionAction.UPDATE,
                "ORDER_SHIPMENT",
                order.getId(),
                beforeSummary,
                afterSummary,
                "Updated shipment for " + safeOrderLabel(order)
        );

        return toResponse(order);
    }

    private String shipmentSummary(CustomerOrder order) {
        return "courier=" + (order.getCourierName() == null ? "" : order.getCourierName())
                + ";awb=" + (order.getTrackingNumber() == null ? "" : order.getTrackingNumber())
                + ";url=" + (order.getTrackingUrl() == null ? "" : order.getTrackingUrl())
                + ";shippedAt=" + (order.getShippedAt() == null ? "" : order.getShippedAt())
                + ";status=" + order.getStatus();
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    public byte[] generateInvoicePdfForUser(Long orderId) {
        User user = userContextService.getCurrentUser();
        return generateSimplePdf(findOwnedOrder(user, orderId));
    }

    public byte[] generateInvoicePdfForAdmin(User admin, Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        requireOrderAccess(admin, order);
        return generateSimplePdf(order);
    }

    public byte[] generateInvoiceWordForUser(Long orderId) {
        User user = userContextService.getCurrentUser();
        return generateInvoiceWord(findOwnedOrder(user, orderId));
    }

    public byte[] generateInvoiceWordForAdmin(User admin, Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        requireOrderAccess(admin, order);
        return generateInvoiceWord(order);
    }

    public List<PaymentWebhookEventResponse> getPaymentWebhookEvents(User admin) {
        permissionService.requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        return paymentWebhookEventRepository.findTop200ByOrderByCreatedAtDescIdDesc().stream()
                .map(event -> PaymentWebhookEventResponse.builder()
                        .id(event.getId())
                        .gatewayEventId(event.getGatewayEventId())
                        .gateway(event.getGateway())
                        .eventType(event.getEventType())
                        .status(event.getStatus())
                        .gatewayOrderId(event.getGatewayOrderId())
                        .gatewayPaymentId(event.getGatewayPaymentId())
                        .errorMessage(event.getErrorMessage())
                        .processedAt(event.getProcessedAt())
                        .createdAt(event.getCreatedAt())
                        .build())
                .toList();
    }

    public List<RefundTransactionResponse> getRefundTransactions(User admin, Long orderId) {
        permissionService.requirePermission(admin, Module.ORDERS, PermissionAction.VIEW);
        return refundTransactionRepository.findByOrderIdOrderByCreatedAtDescIdDesc(orderId).stream()
                .map(refund -> RefundTransactionResponse.builder()
                        .id(refund.getId())
                        .orderId(refund.getOrder().getId())
                        .paymentTransactionId(refund.getPaymentTransaction() != null ? refund.getPaymentTransaction().getId() : null)
                        .refundId(refund.getRefundId())
                        .amount(refund.getAmount())
                        .status(refund.getStatus())
                        .reason(refund.getReason())
                        .refundedAt(refund.getRefundedAt())
                        .createdAt(refund.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void handleRazorpayWebhook(String payload, String signature, String eventId, String userAgent) {
        razorpayService.verifyWebhookSignature(payload, signature);
        if (eventId != null && paymentWebhookEventRepository.findByGatewayEventId(eventId).isPresent()) {
            return;
        }

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
        PaymentWebhookEvent webhookEvent = new PaymentWebhookEvent();
        webhookEvent.setGatewayEventId(eventId);
        webhookEvent.setEventType(event);
        webhookEvent.setGatewayPaymentId(gatewayPaymentId);
        webhookEvent.setGatewayOrderId(gatewayOrderId);
        webhookEvent.setUserAgent(userAgent);
        webhookEvent.setPayload(payload);

        if (gatewayPaymentId == null && gatewayOrderId == null) {
            webhookEvent.setStatus("IGNORED");
            webhookEvent.setErrorMessage("Missing gateway payment/order id");
            webhookEvent.setProcessedAt(LocalDateTime.now());
            paymentWebhookEventRepository.save(webhookEvent);
            return;
        }

        PaymentTransaction transaction = gatewayPaymentId != null
                ? paymentTransactionRepository.findFirstByGatewayPaymentIdOrderByCreatedAtDescIdDesc(gatewayPaymentId).orElse(null)
                : null;
        if (transaction == null && gatewayOrderId != null) {
            transaction = paymentTransactionRepository.findFirstByGatewayOrderIdOrderByCreatedAtDescIdDesc(gatewayOrderId).orElse(null);
        }
        if (transaction == null) {
            webhookEvent.setStatus("UNMATCHED");
            webhookEvent.setErrorMessage("No matching payment transaction");
            webhookEvent.setProcessedAt(LocalDateTime.now());
            paymentWebhookEventRepository.save(webhookEvent);
            return;
        }
        webhookEvent.setStatus("PROCESSED");
        webhookEvent.setProcessedAt(LocalDateTime.now());
        paymentWebhookEventRepository.save(webhookEvent);

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
            boolean orderConfirmed = promoteToConfirmedIfPending(order);
            customerOrderRepository.save(order);
            notificationService.logOrderEvent("PAYMENT_SUCCESS", order, "Payment captured", "Payment for " + order.getOrderNumber() + " was captured.");

            orderTimelineService.record(
                    order,
                    OrderTimelineEventType.PAYMENT_CAPTURED,
                    "Payment captured",
                    "Razorpay webhook confirmed that the payment was captured.",
                    null,
                    "WEBHOOK",
                    metadata("gatewayPaymentId", gatewayPaymentId)
            );
            if (orderConfirmed) {
                orderTimelineService.record(
                        order,
                        OrderTimelineEventType.CONFIRMED,
                        "Order confirmed",
                        "Payment verification completed and the order is now confirmed.",
                        null,
                        "WEBHOOK"
                );
            }
            return;
        }

        if ("payment.failed".equals(event)) {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setFailureReason(extractFailureReason(paymentEntity));
            paymentTransactionRepository.save(transaction);

            order.setPaymentStatus(PaymentStatus.FAILED);
            customerOrderRepository.save(order);
            notificationService.logOrderEvent("PAYMENT_FAILED", order, "Payment failed", "Payment failed for " + order.getOrderNumber() + ".");

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
            Map<String, Object> refundEntity = nestedEntity(payloadMap, "refund");
            String refundId = stringValue(refundEntity.get("id"), null);
            BigDecimal refundedAmount = paiseToRupees(refundEntity.get("amount"));
            transaction.setStatus(PaymentTransactionStatus.REFUNDED);
            transaction.setRefundedAt(LocalDateTime.now());
            transaction.setRefundId(refundId);
            transaction.setRefundedAmount(refundedAmount);
            transaction.setRefundStatus(stringValue(refundEntity.get("status"), "processed"));
            transaction.setRefundReason(stringValue(refundEntity.get("notes"), "Razorpay refund webhook"));
            paymentTransactionRepository.save(transaction);
            recordRefund(order, transaction, refundId, refundedAmount, transaction.getRefundStatus(), transaction.getRefundReason());

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

    @Transactional
    public OrderResponse handleCourierWebhook(CourierWebhookRequest request) {
        CustomerOrder order = request.getOrderNumber() != null && !request.getOrderNumber().isBlank()
                ? customerOrderRepository.findByOrderNumberIgnoreCase(request.getOrderNumber()).orElse(null)
                : null;
        if (order == null && request.getTrackingNumber() != null) {
            order = customerOrderRepository.findAll().stream()
                    .filter(item -> request.getTrackingNumber().equalsIgnoreCase(item.getTrackingNumber()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found for courier webhook"));
        }
        if (order == null) {
            throw new ResourceNotFoundException("Order not found for courier webhook");
        }
        if (request.getCourierName() != null) {
            order.setCourierName(trimToNull(request.getCourierName()));
        }
        if (request.getTrackingNumber() != null) {
            order.setTrackingNumber(trimToNull(request.getTrackingNumber()));
        }
        String normalized = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("DELIVER")) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());
        } else if (normalized.contains("SHIP") || normalized.contains("TRANSIT")) {
            order.setStatus(OrderStatus.SHIPPED);
            if (order.getShippedAt() == null) {
                order.setShippedAt(LocalDateTime.now());
            }
        }
        CustomerOrder saved = customerOrderRepository.save(order);
        orderTimelineService.record(saved, timelineTypeForStatus(saved.getStatus()), "Courier status updated", "Courier reported: " + normalized, null, "COURIER");
        return toResponse(saved);
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
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .deliveryCharge(order.getDeliveryCharge())
                .couponCode(order.getCouponCode())
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
                .deliveryState(order.getDeliveryState())
                .deliveryPostalCode(order.getDeliveryPostalCode())
                .promisedMinDeliveryDays(order.getPromisedMinDeliveryDays())
                .promisedMaxDeliveryDays(order.getPromisedMaxDeliveryDays())
                .notes(order.getNotes())
                .cancellationReason(order.getCancellationReason())
                .returnReason(order.getReturnReason())
                .returnResolutionNote(order.getReturnResolutionNote())
                .paidAt(order.getPaidAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .returnRequestedAt(order.getReturnRequestedAt())
                .shippedAt(order.getShippedAt())
                .courierName(order.getCourierName())
                .trackingNumber(order.getTrackingNumber())
                .trackingUrl(order.getTrackingUrl())
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
                .refundId(transaction.getRefundId())
                .refundedAmount(transaction.getRefundedAmount())
                .refundReason(transaction.getRefundReason())
                .refundStatus(transaction.getRefundStatus())
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
        validateProductStore(cartItem.getProduct(), store);
    }

    private void validateProductStore(Product product, Store store) {
        boolean availableInStore = product.getStores().stream()
                .anyMatch(mappedStore -> mappedStore.getId().equals(store.getId()) && mappedStore.isActive());
        if (!availableInStore) {
            throw new BadRequestException(product.getTitle() + " is not mapped to the selected store");
        }
    }

    private void reserveInventory(Product product, Store store, int quantity) {
        var storeStock = store == null ? java.util.Optional.<com.vrtechnologies.vrtech.entity.ProductStoreStock>empty() : productStoreStockRepository.findByProductIdAndStoreId(product.getId(), store.getId());
        int currentStoreStock = storeStock.map(row -> row.getStockQuantity() == null ? 0 : row.getStockQuantity()).orElse(product.getStockQuantity() == null ? 0 : product.getStockQuantity());
        if (!product.isAvailable() || currentStoreStock < quantity) {
            throw new BadRequestException(product.getTitle() + " does not have enough stock");
        }
        storeStock.ifPresent(row -> {
            row.setStockQuantity(currentStoreStock - quantity);
            productStoreStockRepository.save(row);
        });
        int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        product.setStockQuantity(currentStock - quantity);
        if (product.getStockQuantity() <= 0) {
            product.setAvailable(false);
        }
    }

    private void releaseInventory(Product product, Store store, int quantity) {
        if (store != null) {
            productStoreStockRepository.findByProductIdAndStoreId(product.getId(), store.getId()).ifPresent(row -> {
                row.setStockQuantity((row.getStockQuantity() == null ? 0 : row.getStockQuantity()) + quantity);
                productStoreStockRepository.save(row);
            });
        }
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
            order.setInvoiceNumber(allocateInvoiceNumber(order.getId()));
            changed = true;
        }
        if (changed) {
            customerOrderRepository.save(order);
        }
    }

    private synchronized String allocateInvoiceNumber(Long orderId) {
        SiteSettings settings = siteSettingsRepository.findTopByOrderByIdAsc().orElse(null);
        if (settings == null) {
            return String.format("VRT-INV-%06d", orderId);
        }
        String prefix = settings.getInvoicePrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "INV-";
        }
        int padding = settings.getInvoicePadding() == null
                ? 6
                : Math.max(1, Math.min(12, settings.getInvoicePadding()));
        long sequence = settings.getInvoiceNextSequence() == null || settings.getInvoiceNextSequence() < 1L
                ? 1L
                : settings.getInvoiceNextSequence();
        String number = prefix + String.format("%0" + padding + "d", sequence);
        settings.setInvoiceNextSequence(sequence + 1L);
        siteSettingsRepository.save(settings);
        return number;
    }

    private boolean canCancel(CustomerOrder order) {
        return switch (order.getStatus()) {
            case PENDING, CONFIRMED, PACKED -> true;
            default -> false;
        };
    }

    private boolean promoteToConfirmedIfPending(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            return true;
        }
        return false;
    }

    private void updateUserCheckoutPreferences(User user, OrderRequest request) {
        user.setPreferredContactName(trimToNull(request.getContactName()));
        user.setPreferredContactPhone(trimToNull(request.getContactPhone()));
        user.setPreferredContactEmail(sanitizeContactEmail(request.getContactEmail()));
        if (request.getDeliveryType() != null && request.getDeliveryType().name().equals("DELIVERY")) {
            user.setPreferredDeliveryAddress(trimToNull(request.getDeliveryAddress()));
        }
    }

    private PincodeDeliveryService.DeliveryResolution resolveDelivery(SiteSettings settings, OrderRequest request, Store store, BigDecimal afterDiscount) {
        if (request.getDeliveryType() == null || !request.getDeliveryType().name().equals("DELIVERY")) {
            return PincodeDeliveryService.DeliveryResolution.serviceable(true, true, BigDecimal.ZERO, null, false, null, null, "PICKUP", "Pickup order");
        }
        boolean codRequested = request.getPaymentMethod() != null && request.getPaymentMethod().name().equals("CASH");
        PincodeDeliveryService.DeliveryResolution resolution = pincodeDeliveryService.resolveDelivery(
                settings,
                request.getDeliveryPostalCode(),
                request.getDeliveryState(),
                afterDiscount,
                store == null ? null : store.getId(),
                codRequested
        );
        if (!resolution.isServiceable()) {
            throw new BadRequestException(resolution.getMessage());
        }
        return resolution;
    }

    private List<CheckoutProfileResponse.SavedAddressResponse> buildSavedAddresses(User user, List<CustomerOrder> recentOrders) {
        List<CheckoutProfileResponse.SavedAddressResponse> savedAddresses = new ArrayList<>();
        LinkedHashSet<String> seenAddresses = new LinkedHashSet<>();
        String preferredAddress = trimToNull(user.getPreferredDeliveryAddress());

        userAddressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDescIdDesc(user.getId()).forEach(address -> {
            String normalizedAddress = trimToNull(address.getAddress());
            if (normalizedAddress == null || !seenAddresses.add(normalizedAddress)) {
                return;
            }
            savedAddresses.add(CheckoutProfileResponse.SavedAddressResponse.builder()
                    .id("address-" + address.getId())
                    .label(address.getLabel())
                    .address(normalizedAddress)
                    .contactName(trimToNull(address.getContactName()))
                    .contactPhone(trimToNull(address.getContactPhone()))
                    .state(trimToNull(address.getState()))
                    .postalCode(trimToNull(address.getPostalCode()))
                    .defaultAddress(address.isDefaultAddress())
                    .build());
        });

        for (CustomerOrder order : recentOrders) {
            String address = trimToNull(order.getDeliveryAddress());
            if (address == null || !seenAddresses.add(address)) {
                continue;
            }

            boolean defaultAddress = preferredAddress != null && preferredAddress.equals(address);
            savedAddresses.add(CheckoutProfileResponse.SavedAddressResponse.builder()
                    .id("order-" + order.getId())
                    .label(savedAddresses.isEmpty() ? "Previously used" : "Saved address")
                    .address(address)
                    .contactName(trimToNull(order.getContactName()))
                    .contactPhone(trimToNull(order.getContactPhone()))
                    .state(trimToNull(order.getDeliveryState()))
                    .postalCode(trimToNull(order.getDeliveryPostalCode()))
                    .defaultAddress(defaultAddress)
                    .build());
        }

        return savedAddresses;
    }

    private void cancelOrder(CustomerOrder order, String reason, User actor, String source) {
        if (order.getStatus() != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                releaseInventory(item.getProduct(), order.getStore(), item.getQuantity());
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
            transaction.setRefundedAmount(order.getTotalAmount());
            transaction.setRefundStatus("RECORDED");
            transaction.setRefundReason(description);
        }
        transaction.setMetadata(metadata("description", description, "actorEmail", admin.getEmail()));
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        if (status == PaymentTransactionStatus.REFUNDED) {
            recordRefund(order, saved, saved.getRefundId(), saved.getRefundedAmount(), saved.getRefundStatus(), description);
        }
    }

    private void recordRefund(CustomerOrder order, PaymentTransaction transaction, String refundId, BigDecimal amount, String status, String reason) {
        RefundTransaction refund = new RefundTransaction();
        refund.setOrder(order);
        refund.setPaymentTransaction(transaction);
        refund.setRefundId(refundId);
        refund.setAmount(amount == null || amount.signum() <= 0 ? order.getTotalAmount() : amount);
        refund.setStatus(status == null || status.isBlank() ? "RECORDED" : status.toUpperCase(Locale.ROOT));
        refund.setReason(reason);
        refund.setRefundedAt(LocalDateTime.now());
        refundTransactionRepository.save(refund);
    }

    private BigDecimal paiseToRupees(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isOnlinePaymentMethod(com.vrtechnologies.vrtech.entity.enums.PaymentMethod paymentMethod) {
        return paymentMethod != null && !paymentMethod.name().equals("CASH");
    }

    private boolean withinDateRange(LocalDateTime value, LocalDateTime startDate, LocalDateTime endDate) {
        if (value == null) {
            return false;
        }
        return (startDate == null || !value.isBefore(startDate)) && (endDate == null || value.isBefore(endDate));
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

        SiteSettings settings = siteSettingsRepository.findTopByOrderByIdAsc().orElseGet(SiteSettings::new);
        boolean gstEnabled = settings.isGstEnabled();
        BigDecimal gstRate = settings.getGstRate() == null ? BigDecimal.ZERO : settings.getGstRate();
        String defaultHsn = settings.getDefaultHsnCode();

        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            BigDecimal lineTotal = item.getPriceAtTime().multiply(qty);
            String hsn = defaultString(item.getProduct().getHsnCode(), defaultString(defaultHsn, "-"));
            itemsHtml.append("""
                    <tr>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(item.getProduct().getTitle()),
                    escapeHtml(hsn),
                    item.getQuantity(),
                    formatCurrency(item.getPriceAtTime()),
                    formatCurrency(lineTotal)
            ));
        }

        BigDecimal subtotal = order.getSubtotalAmount() == null ? BigDecimal.ZERO : order.getSubtotalAmount();
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        BigDecimal tax = order.getTaxAmount() == null ? BigDecimal.ZERO : order.getTaxAmount();
        BigDecimal delivery = order.getDeliveryCharge() == null ? BigDecimal.ZERO : order.getDeliveryCharge();
        BigDecimal grand = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();

        // Determine intra/inter-state for SGST/CGST vs IGST split. If we have both
        // a company state and a delivery state, and they differ, treat as inter-state (IGST).
        String companyState = defaultString(settings.getDefaultState(), "");
        String customerState = defaultString(order.getDeliveryState(), "");
        boolean interState = !companyState.isBlank() && !customerState.isBlank()
                && !companyState.equalsIgnoreCase(customerState);
        BigDecimal halfTax = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        String taxBreakdown;
        if (gstEnabled && tax.signum() > 0) {
            if (interState) {
                taxBreakdown = "<tr><td colspan='4'>IGST @ " + formatPercent(gstRate) + "</td><td>"
                        + formatCurrency(tax) + "</td></tr>";
            } else {
                taxBreakdown = "<tr><td colspan='4'>CGST @ " + formatPercent(gstRate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) + "</td><td>"
                        + formatCurrency(halfTax) + "</td></tr>"
                        + "<tr><td colspan='4'>SGST @ " + formatPercent(gstRate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) + "</td><td>"
                        + formatCurrency(halfTax) + "</td></tr>";
            }
        } else {
            taxBreakdown = "";
        }

        String companyName = defaultString(settings.getCompanyName(), "VR Technologies");
        String companyAddressLine = defaultString(settings.getCompanyAddress(), "");
        String companyCityState = String.join(", ",
                List.of(
                        defaultString(settings.getDefaultCity(), ""),
                        defaultString(settings.getDefaultState(), ""),
                        defaultString(settings.getCompanyPincode(), "")
                ).stream().filter(s -> !s.isBlank()).toList());
        String gstinLine = settings.getGstNumber() != null && !settings.getGstNumber().isBlank()
                ? "GSTIN: " + escapeHtml(settings.getGstNumber()) : "";
        String panLine = settings.getCompanyPan() != null && !settings.getCompanyPan().isBlank()
                ? "PAN: " + escapeHtml(settings.getCompanyPan()) : "";
        String supportLine = String.join(" · ",
                List.of(
                        defaultString(settings.getSupportEmail(), ""),
                        defaultString(settings.getSupportPhone(), "")
                ).stream().filter(s -> !s.isBlank()).toList());
        String terms = settings.getInvoiceTerms() == null || settings.getInvoiceTerms().isBlank()
                ? "Thank you for shopping with " + escapeHtml(companyName) + ". This invoice was generated digitally and is valid without a signature."
                : escapeHtml(settings.getInvoiceTerms());

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
                    .gst-pill { display: inline-block; margin-top: 6px; padding: 4px 10px; border-radius: 999px; background: #ecfdf5; color: #065f46; font-size: 11px; font-weight: 700; letter-spacing: 0.14em; text-transform: uppercase; }
                    h1 { margin: 16px 0 8px; font-size: 34px; }
                    h2 { font-size: 18px; margin: 0 0 12px; }
                    .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; margin-top: 28px; }
                    .card { border: 1px solid rgba(30, 58, 138, 0.08); background: #f8fbff; border-radius: 18px; padding: 18px; }
                    .meta { font-size: 13px; color: #475569; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 28px; }
                    th, td { padding: 12px 10px; border-bottom: 1px solid #e2e8f0; text-align: left; font-size: 14px; }
                    th { font-size: 11px; text-transform: uppercase; letter-spacing: 0.14em; color: #64748b; }
                    .totals { margin-top: 28px; width: 100%%; max-width: 360px; margin-left: auto; }
                    .totals td { padding: 8px 10px; border: none; }
                    .totals tr.grand td { border-top: 2px solid #0f172a; font-weight: 700; font-size: 18px; }
                    .footer { margin-top: 32px; font-size: 13px; color: #64748b; line-height: 1.6; white-space: pre-line; }
                    @media print { body { background: white; } .wrap { box-shadow: none; margin: 0; max-width: none; border-radius: 0; } }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="header">
                      <div>
                        <div class="badge">%s</div>
                        <h1>%s</h1>
                        <div>Order %s</div>
                        <div>Issued %s</div>
                      </div>
                      <div>
                        <h2>%s</h2>
                        <div class="meta">%s</div>
                        <div class="meta">%s</div>
                        <div class="meta">%s</div>
                        %s
                        %s
                        %s
                      </div>
                    </div>

                    <div class="grid">
                      <div class="card">
                        <h2>Bill To</h2>
                        <div>%s</div>
                        <div class="meta">%s</div>
                        <div class="meta">%s</div>
                        <div class="meta">%s</div>
                      </div>
                      <div class="card">
                        <h2>Fulfilment</h2>
                        <div class="meta">Status: %s</div>
                        <div class="meta">Payment: %s</div>
                        <div class="meta">Method: %s</div>
                        <div class="meta">Store: %s</div>
                      </div>
                    </div>

                    <table>
                      <thead>
                        <tr>
                          <th>Product</th>
                          <th>HSN/SAC</th>
                          <th>Qty</th>
                          <th>Unit Price</th>
                          <th>Line Total</th>
                        </tr>
                      </thead>
                      <tbody>
                        %s
                      </tbody>
                    </table>

                    <table class="totals">
                      <tr><td colspan="4">Subtotal</td><td>%s</td></tr>
                      %s
                      %s
                      %s
                      <tr class="grand"><td colspan="4">Grand Total</td><td>%s</td></tr>
                    </table>

                    <div class="footer">
                      %s
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(order.getInvoiceNumber()),
                gstEnabled ? "Tax Invoice" : "Invoice",
                escapeHtml(order.getInvoiceNumber()),
                escapeHtml(order.getOrderNumber()),
                DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").format(order.getCreatedAt()),
                escapeHtml(companyName),
                escapeHtml(companyAddressLine.isBlank() ? "Refurbished systems marketplace" : companyAddressLine),
                escapeHtml(companyCityState),
                escapeHtml(supportLine),
                gstinLine.isBlank() ? "" : "<div class='gst-pill'>" + gstinLine + "</div>",
                panLine.isBlank() ? "" : "<div class='meta'>" + panLine + "</div>",
                gstEnabled ? "<div class='meta'>GST: " + formatPercent(gstRate) + "</div>" : "",
                escapeHtml(defaultString(order.getContactName(), "Customer")),
                escapeHtml(defaultString(order.getContactPhone(), "-")),
                escapeHtml(defaultString(order.getContactEmail(), "-")),
                escapeHtml(defaultString(order.getDeliveryType() != null && order.getDeliveryType().name().equals("DELIVERY") ? order.getDeliveryAddress() : order.getStore() != null ? order.getStore().getAddress() : "-", "-")),
                escapeHtml(order.getStatus().name()),
                escapeHtml(order.getPaymentStatus().name()),
                escapeHtml(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "-"),
                escapeHtml(order.getStore() != null ? order.getStore().getName() : "Not assigned"),
                itemsHtml,
                formatCurrency(subtotal),
                discount.signum() > 0 ? "<tr><td colspan='4'>Discount</td><td>- " + formatCurrency(discount) + "</td></tr>" : "",
                taxBreakdown,
                delivery.signum() > 0 ? "<tr><td colspan='4'>Delivery</td><td>" + formatCurrency(delivery) + "</td></tr>" : "",
                formatCurrency(grand),
                terms
        );
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) return "0%";
        return value.stripTrailingZeros().toPlainString() + "%";
    }

    private byte[] generateSimplePdf(CustomerOrder order) {
        ensureOrderIdentifiers(order);
        SiteSettings settings = siteSettingsRepository.findTopByOrderByIdAsc().orElseGet(SiteSettings::new);
        String layout = settings.getInvoiceLayout() != null ? settings.getInvoiceLayout().toUpperCase() : "MINIMAL";
        boolean gstEnabled = settings.isGstEnabled();
        BigDecimal gstRate = settings.getGstRate() != null ? settings.getGstRate() : BigDecimal.valueOf(18);
        String defaultHsn = settings.getDefaultHsnCode();

        String companyName = defaultString(settings.getCompanyName(), "VR Technologies");
        String companyCity = defaultString(settings.getDefaultCity(), "");
        String companyState = defaultString(settings.getDefaultState(), "");
        String termsText = defaultString(settings.getInvoiceTerms(), "Thank you for shopping with " + companyName + "!");
        
        String invoiceNum = defaultString(order.getInvoiceNumber(), "-");
        String orderNum = defaultString(order.getOrderNumber(), "-");
        String customerName = defaultString(order.getContactName(), "-");
        String customerPhone = defaultString(order.getContactPhone(), "-");
        String customerEmail = defaultString(order.getContactEmail(), "-");
        String deliveryAddress = defaultString(order.getDeliveryType() != null && order.getDeliveryType().name().equals("DELIVERY") ? order.getDeliveryAddress() : "Store Pickup", "-");
        String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING";
        String orderStatus = order.getStatus() != null ? order.getStatus().name() : "PENDING";
        String dateStr = order.getCreatedAt() != null ? DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").format(order.getCreatedAt()) : "-";
        
        StringBuilder pdfStream = new StringBuilder();

        // 1. Draw Graphics & Background Colors
        if ("NEON".equals(layout)) {
            // Background fill (charcoal almost black)
            pdfStream.append("0.06 0.08 0.12 rg\n");
            pdfStream.append("0 0 595 842 re f\n");
            
            // Border around the page
            pdfStream.append("0 1 1 RG\n");
            pdfStream.append("1 w\n");
            pdfStream.append("30 30 535 782 re S\n");
            
            // Header card
            pdfStream.append("0.09 0.13 0.2 rg\n");
            pdfStream.append("40 720 515 90 re f\n");
            pdfStream.append("40 720 515 90 re S\n");
            
            // Bill To and Fulfilment cards
            pdfStream.append("45 595 240 100 re f\n");
            pdfStream.append("45 595 240 100 re S\n");
            pdfStream.append("310 595 240 100 re f\n");
            pdfStream.append("310 595 240 100 re S\n");
            
            // Table Header background
            pdfStream.append("0.08 0.12 0.18 rg\n");
            pdfStream.append("40 565 515 20 re f\n");
            pdfStream.append("40 565 515 20 re S\n");
        } else if ("MODERN".equals(layout)) {
            // Header card (deep indigo)
            pdfStream.append("0.18 0.21 0.6 rg\n");
            pdfStream.append("40 720 515 90 re f\n");
            
            // Bill To and Fulfilment cards
            pdfStream.append("0.95 0.97 0.99 rg\n");
            pdfStream.append("0.85 0.88 0.95 RG\n");
            pdfStream.append("0.5 w\n");
            pdfStream.append("45 595 240 100 re f\n");
            pdfStream.append("45 595 240 100 re S\n");
            pdfStream.append("310 595 240 100 re f\n");
            pdfStream.append("310 595 240 100 re S\n");
            
            // Table Header background
            pdfStream.append("0.92 0.94 0.98 rg\n");
            pdfStream.append("40 565 515 20 re f\n");
        } else {
            // MINIMAL
            // Header line
            pdfStream.append("0.7 0.7 0.7 RG\n");
            pdfStream.append("0.5 w\n");
            pdfStream.append("50 710 m 545 710 l S\n");
        }

        // 2. Draw Text Content
        // Header Left (Company Name, Tagline, Location)
        if ("NEON".equals(layout)) {
            pdfStream.append("BT\n/F2 16 Tf\n1 g\n55 780 Td\n(").append(escapePdf(companyName)).append(") Tj\n");
            pdfStream.append("/F1 9 Tf\n0 1 1 rg\n0 -16 Td\n(").append(escapePdf(settings.getTagline() != null ? settings.getTagline() : "Refurbished. Warranted. Trusted.")).append(") Tj\n");
            pdfStream.append("0.7 0.8 0.95 rg\n0 -14 Td\n(").append(escapePdf(companyCity + ", " + companyState)).append(") Tj\nET\n");
            
            // Header Right (TAX INVOICE, Inv #, Date)
            pdfStream.append("BT\n/F2 12 Tf\n0 1 1 rg\n350 780 Td\n(TAX INVOICE) Tj\n");
            pdfStream.append("/F1 9 Tf\n1 g\n0 -16 Td\n(Invoice: ").append(escapePdf(invoiceNum)).append(") Tj\n");
            pdfStream.append("0 -14 Td\n(Order: ").append(escapePdf(orderNum)).append(") Tj\n");
            pdfStream.append("0 -14 Td\n(Date: ").append(escapePdf(dateStr)).append(") Tj\nET\n");
        } else if ("MODERN".equals(layout)) {
            pdfStream.append("BT\n/F2 16 Tf\n1 g\n55 780 Td\n(").append(escapePdf(companyName)).append(") Tj\n");
            pdfStream.append("/F1 9 Tf\n0 -16 Td\n(").append(escapePdf(settings.getTagline() != null ? settings.getTagline() : "Refurbished. Warranted. Trusted.")).append(") Tj\n");
            pdfStream.append("0 -14 Td\n(").append(escapePdf(companyCity + ", " + companyState)).append(") Tj\nET\n");
            
            pdfStream.append("BT\n/F2 12 Tf\n1 g\n350 780 Td\n(TAX INVOICE) Tj\n");
            pdfStream.append("/F1 9 Tf\n0 -16 Td\n(Invoice: ").append(escapePdf(invoiceNum)).append(") Tj\n");
            pdfStream.append("0 -14 Td\n(Order: ").append(escapePdf(orderNum)).append(") Tj\n");
            pdfStream.append("0 -14 Td\n(Date: ").append(escapePdf(dateStr)).append(") Tj\nET\n");
        } else {
            // MINIMAL
            pdfStream.append("BT\n/F2 16 Tf\n0 g\n55 780 Td\n(").append(escapePdf(companyName)).append(") Tj\n");
            pdfStream.append("/F1 9 Tf\n0.3 g\n0 -16 Td\n(").append(escapePdf(settings.getTagline() != null ? settings.getTagline() : "Refurbished. Warranted. Trusted.")).append(") Tj\n");
            pdfStream.append("0 -14 Td\n(").append(escapePdf(companyCity + ", " + companyState)).append(") Tj\nET\n");
            
            pdfStream.append("BT\n/F2 12 Tf\n0 g\n350 780 Td\n(TAX INVOICE) Tj\n");
            pdfStream.append("/F1 9 Tf\n0.3 g\n0 -16 Td\n(Invoice: ").append(escapePdf(invoiceNum)).append(") Tj\n");
            pdfStream.append("0 -14 Td\n(Order: ").append(escapePdf(orderNum)).append(") Tj\n");
            pdfStream.append("0 -14 Td\n(Date: ").append(escapePdf(dateStr)).append(") Tj\nET\n");
        }

        // Billing & Fulfilment information text columns
        if ("NEON".equals(layout)) {
            pdfStream.append("BT\n/F2 10 Tf\n0 1 1 rg\n55 675 Td\n(BILL TO:) Tj\n");
            pdfStream.append("/F1 9 Tf\n1 g\n0 -14 Td\n(").append(escapePdf(customerName)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Phone: ").append(escapePdf(customerPhone)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Email: ").append(escapePdf(customerEmail)).append(") Tj\n");
            String trimmedAddr = deliveryAddress.length() > 30 ? deliveryAddress.substring(0, 27) + "..." : deliveryAddress;
            pdfStream.append("0 -12 Td\n(").append(escapePdf(trimmedAddr)).append(") Tj\nET\n");
            
            pdfStream.append("BT\n/F2 10 Tf\n0 1 1 rg\n320 675 Td\n(FULFILMENT & PAYMENT:) Tj\n");
            pdfStream.append("/F1 9 Tf\n1 g\n0 -14 Td\n(Order Status: ").append(escapePdf(orderStatus)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Payment: ").append(escapePdf(paymentStatus)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Method: ").append(escapePdf(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "-")).append(") Tj\n");
            String storeName = order.getStore() != null ? order.getStore().getName() : "Not assigned";
            pdfStream.append("0 -12 Td\n(Store: ").append(escapePdf(storeName)).append(") Tj\nET\n");
        } else if ("MODERN".equals(layout)) {
            pdfStream.append("BT\n/F2 10 Tf\n0.18 0.21 0.6 rg\n55 675 Td\n(BILL TO:) Tj\n");
            pdfStream.append("/F1 9 Tf\n0.09 0.12 0.2 rg\n0 -14 Td\n(").append(escapePdf(customerName)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Phone: ").append(escapePdf(customerPhone)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Email: ").append(escapePdf(customerEmail)).append(") Tj\n");
            String trimmedAddr = deliveryAddress.length() > 30 ? deliveryAddress.substring(0, 27) + "..." : deliveryAddress;
            pdfStream.append("0 -12 Td\n(").append(escapePdf(trimmedAddr)).append(") Tj\nET\n");
            
            pdfStream.append("BT\n/F2 10 Tf\n0.18 0.21 0.6 rg\n320 675 Td\n(FULFILMENT & PAYMENT:) Tj\n");
            pdfStream.append("/F1 9 Tf\n0.09 0.12 0.2 rg\n0 -14 Td\n(Order Status: ").append(escapePdf(orderStatus)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Payment: ").append(escapePdf(paymentStatus)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Method: ").append(escapePdf(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "-")).append(") Tj\n");
            String storeName = order.getStore() != null ? order.getStore().getName() : "Not assigned";
            pdfStream.append("0 -12 Td\n(Store: ").append(escapePdf(storeName)).append(") Tj\nET\n");
        } else {
            // MINIMAL
            pdfStream.append("BT\n/F2 10 Tf\n0 g\n55 675 Td\n(BILL TO:) Tj\n");
            pdfStream.append("/F1 9 Tf\n0.3 g\n0 -14 Td\n(").append(escapePdf(customerName)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Phone: ").append(escapePdf(customerPhone)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Email: ").append(escapePdf(customerEmail)).append(") Tj\n");
            String trimmedAddr = deliveryAddress.length() > 30 ? deliveryAddress.substring(0, 27) + "..." : deliveryAddress;
            pdfStream.append("0 -12 Td\n(").append(escapePdf(trimmedAddr)).append(") Tj\nET\n");
            
            pdfStream.append("BT\n/F2 10 Tf\n0 g\n320 675 Td\n(FULFILMENT & PAYMENT:) Tj\n");
            pdfStream.append("/F1 9 Tf\n0.3 g\n0 -14 Td\n(Order Status: ").append(escapePdf(orderStatus)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Payment: ").append(escapePdf(paymentStatus)).append(") Tj\n");
            pdfStream.append("0 -12 Td\n(Method: ").append(escapePdf(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "-")).append(") Tj\n");
            String storeName = order.getStore() != null ? order.getStore().getName() : "Not assigned";
            pdfStream.append("0 -12 Td\n(Store: ").append(escapePdf(storeName)).append(") Tj\nET\n");
        }

        // Table Header labels
        if ("NEON".equals(layout)) {
            pdfStream.append("BT\n/F2 9 Tf\n0 1 1 rg\n");
            pdfStream.append("55 571 Td (PRODUCT TITLE) Tj\n");
            pdfStream.append("265 0 Td (HSN) Tj\n");
            pdfStream.append("60 0 Td (QTY) Tj\n");
            pdfStream.append("60 0 Td (UNIT PRICE) Tj\n");
            pdfStream.append("55 0 Td (TOTAL) Tj\nET\n");
        } else if ("MODERN".equals(layout)) {
            pdfStream.append("BT\n/F2 9 Tf\n0.18 0.21 0.6 rg\n");
            pdfStream.append("55 571 Td (PRODUCT TITLE) Tj\n");
            pdfStream.append("265 0 Td (HSN) Tj\n");
            pdfStream.append("60 0 Td (QTY) Tj\n");
            pdfStream.append("60 0 Td (UNIT PRICE) Tj\n");
            pdfStream.append("55 0 Td (TOTAL) Tj\nET\n");
        } else {
            // MINIMAL
            pdfStream.append("BT\n/F2 9 Tf\n0 g\n");
            pdfStream.append("55 571 Td (PRODUCT TITLE) Tj\n");
            pdfStream.append("265 0 Td (HSN) Tj\n");
            pdfStream.append("60 0 Td (QTY) Tj\n");
            pdfStream.append("60 0 Td (UNIT PRICE) Tj\n");
            pdfStream.append("55 0 Td (TOTAL) Tj\nET\n");
        }

        // Items Rows
        int y = 545;
        for (OrderItem item : order.getItems()) {
            String title = item.getProduct().getTitle();
            if (title.length() > 30) {
                title = title.substring(0, 27) + "...";
            }
            String hsn = defaultString(item.getProduct().getHsnCode(), defaultString(defaultHsn, "-"));
            BigDecimal lineTotal = item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity()));
            
            if ("NEON".equals(layout)) {
                pdfStream.append("BT\n/F1 9 Tf\n1 g\n55 ").append(y).append(" Td (").append(escapePdf(title)).append(") Tj\n");
                pdfStream.append("265 0 Td (").append(escapePdf(hsn)).append(") Tj\n");
                pdfStream.append("60 0 Td (").append(item.getQuantity()).append(") Tj\n");
                pdfStream.append("60 0 Td (").append(escapePdf(formatCurrency(item.getPriceAtTime()))).append(") Tj\n");
                pdfStream.append("55 0 Td (").append(escapePdf(formatCurrency(lineTotal))).append(") Tj\nET\n");
            } else if ("MODERN".equals(layout)) {
                pdfStream.append("BT\n/F1 9 Tf\n0.09 0.12 0.2 rg\n55 ").append(y).append(" Td (").append(escapePdf(title)).append(") Tj\n");
                pdfStream.append("265 0 Td (").append(escapePdf(hsn)).append(") Tj\n");
                pdfStream.append("60 0 Td (").append(item.getQuantity()).append(") Tj\n");
                pdfStream.append("60 0 Td (").append(escapePdf(formatCurrency(item.getPriceAtTime()))).append(") Tj\n");
                pdfStream.append("55 0 Td (").append(escapePdf(formatCurrency(lineTotal))).append(") Tj\nET\n");
            } else {
                pdfStream.append("BT\n/F1 9 Tf\n0 g\n55 ").append(y).append(" Td (").append(escapePdf(title)).append(") Tj\n");
                pdfStream.append("265 0 Td (").append(escapePdf(hsn)).append(") Tj\n");
                pdfStream.append("60 0 Td (").append(item.getQuantity()).append(") Tj\n");
                pdfStream.append("60 0 Td (").append(escapePdf(formatCurrency(item.getPriceAtTime()))).append(") Tj\n");
                pdfStream.append("55 0 Td (").append(escapePdf(formatCurrency(lineTotal))).append(") Tj\nET\n");
            }
            y -= 18;
        }
        
        int tableBottom = y + 8;
        // Bottom Table rule
        if ("NEON".equals(layout)) {
            pdfStream.append("0 1 1 RG\n");
            pdfStream.append("40 ").append(tableBottom).append(" m 555 ").append(tableBottom).append(" l S\n");
        } else if ("MODERN".equals(layout)) {
            pdfStream.append("0.18 0.21 0.6 RG\n");
            pdfStream.append("40 ").append(tableBottom).append(" m 555 ").append(tableBottom).append(" l S\n");
        } else {
            pdfStream.append("0.7 0.7 0.7 RG\n");
            pdfStream.append("50 ").append(tableBottom).append(" m 545 ").append(tableBottom).append(" l S\n");
        }

        // Totals Section
        y = tableBottom - 20;
        BigDecimal subtotal = order.getSubtotalAmount() != null ? order.getSubtotalAmount() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal tax = order.getTaxAmount() != null ? order.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal delivery = order.getDeliveryCharge() != null ? order.getDeliveryCharge() : BigDecimal.ZERO;
        BigDecimal grand = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;

        List<String[]> totalsLines = new ArrayList<>();
        totalsLines.add(new String[]{"Subtotal:", formatCurrency(subtotal)});
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            totalsLines.add(new String[]{"Discount:", "-" + formatCurrency(discount)});
        }
        if (gstEnabled && tax.compareTo(BigDecimal.ZERO) > 0) {
            String companyStateLoc = defaultString(settings.getDefaultState(), "");
            String customerStateLoc = defaultString(order.getDeliveryState(), "");
            boolean interState = !companyStateLoc.isBlank() && !customerStateLoc.isBlank()
                    && !companyStateLoc.equalsIgnoreCase(customerStateLoc);
            if (interState) {
                totalsLines.add(new String[]{"IGST @ " + formatPercent(gstRate) + ":", formatCurrency(tax)});
            } else {
                BigDecimal halfTax = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                BigDecimal halfRate = gstRate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                totalsLines.add(new String[]{"CGST @ " + formatPercent(halfRate) + ":", formatCurrency(halfTax)});
                totalsLines.add(new String[]{"SGST @ " + formatPercent(halfRate) + ":", formatCurrency(halfTax)});
            }
        }
        if (delivery.compareTo(BigDecimal.ZERO) > 0) {
            totalsLines.add(new String[]{"Delivery Charge:", formatCurrency(delivery)});
        }

        for (String[] line : totalsLines) {
            if ("NEON".equals(layout)) {
                pdfStream.append("BT\n/F1 9 Tf\n0.7 0.8 0.95 rg\n360 ").append(y).append(" Td (").append(escapePdf(line[0])).append(") Tj\n");
                pdfStream.append("135 0 Td (").append(escapePdf(line[1])).append(") Tj\nET\n");
            } else if ("MODERN".equals(layout)) {
                pdfStream.append("BT\n/F1 9 Tf\n0.4 0.45 0.55 rg\n360 ").append(y).append(" Td (").append(escapePdf(line[0])).append(") Tj\n");
                pdfStream.append("135 0 Td (").append(escapePdf(line[1])).append(") Tj\nET\n");
            } else {
                pdfStream.append("BT\n/F1 9 Tf\n0.3 g\n360 ").append(y).append(" Td (").append(escapePdf(line[0])).append(") Tj\n");
                pdfStream.append("135 0 Td (").append(escapePdf(line[1])).append(") Tj\nET\n");
            }
            y -= 14;
        }

        // Grand Total row
        y -= 5;
        if ("NEON".equals(layout)) {
            pdfStream.append("BT\n/F2 11 Tf\n0 1 1 rg\n360 ").append(y).append(" Td (GRAND TOTAL:) Tj\n");
            pdfStream.append("135 0 Td (").append(escapePdf(formatCurrency(grand))).append(") Tj\nET\n");
        } else if ("MODERN".equals(layout)) {
            pdfStream.append("BT\n/F2 11 Tf\n0.18 0.21 0.6 rg\n360 ").append(y).append(" Td (GRAND TOTAL:) Tj\n");
            pdfStream.append("135 0 Td (").append(escapePdf(formatCurrency(grand))).append(") Tj\nET\n");
        } else {
            pdfStream.append("BT\n/F2 11 Tf\n0 g\n360 ").append(y).append(" Td (GRAND TOTAL:) Tj\n");
            pdfStream.append("135 0 Td (").append(escapePdf(formatCurrency(grand))).append(") Tj\nET\n");
        }

        // Terms & Conditions block
        int termsStart = Math.min(y - 30, 160);
        if ("NEON".equals(layout)) {
            pdfStream.append("BT\n/F2 8 Tf\n0 1 1 rg\n55 ").append(termsStart).append(" Td (TERMS & CONDITIONS) Tj\nET\n");
            int termY = termsStart - 12;
            pdfStream.append("BT\n/F1 7 Tf\n10 TL\n0.7 0.8 0.95 rg\n55 ").append(termY).append(" Td\n");
            String[] termsLines = termsText.split("\n");
            for (String line : termsLines) {
                if (line.trim().isEmpty()) continue;
                pdfStream.append("(").append(escapePdf(line.trim())).append(") Tj T* ");
            }
            pdfStream.append("ET\n");
        } else if ("MODERN".equals(layout)) {
            pdfStream.append("BT\n/F2 8 Tf\n0.18 0.21 0.6 rg\n55 ").append(termsStart).append(" Td (TERMS & CONDITIONS) Tj\nET\n");
            int termY = termsStart - 12;
            pdfStream.append("BT\n/F1 7 Tf\n10 TL\n0.4 0.45 0.55 rg\n55 ").append(termY).append(" Td\n");
            String[] termsLines = termsText.split("\n");
            for (String line : termsLines) {
                if (line.trim().isEmpty()) continue;
                pdfStream.append("(").append(escapePdf(line.trim())).append(") Tj T* ");
            }
            pdfStream.append("ET\n");
        } else {
            pdfStream.append("BT\n/F2 8 Tf\n0 g\n55 ").append(termsStart).append(" Td (TERMS & CONDITIONS) Tj\nET\n");
            int termY = termsStart - 12;
            pdfStream.append("BT\n/F1 7 Tf\n10 TL\n0.3 g\n55 ").append(termY).append(" Td\n");
            String[] termsLines = termsText.split("\n");
            for (String line : termsLines) {
                if (line.trim().isEmpty()) continue;
                pdfStream.append("(").append(escapePdf(line.trim())).append(") Tj T* ");
            }
            pdfStream.append("ET\n");
        }

        // Thin footer divider
        if ("NEON".equals(layout)) {
            pdfStream.append("0 1 1 RG\n");
            pdfStream.append("40 45 m 555 45 l S\n");
            pdfStream.append("BT\n/F1 7 Tf\n0.7 0.8 0.95 rg\n55 30 Td (Digitally generated invoice by VR Technologies. Thank you for your business!) Tj\nET\n");
        } else if ("MODERN".equals(layout)) {
            pdfStream.append("0.85 0.88 0.95 RG\n");
            pdfStream.append("40 45 m 555 45 l S\n");
            pdfStream.append("BT\n/F1 7 Tf\n0.4 0.45 0.55 rg\n55 30 Td (Digitally generated invoice by VR Technologies. Thank you for your business!) Tj\nET\n");
        } else {
            pdfStream.append("0.7 0.7 0.7 RG\n");
            pdfStream.append("50 45 m 545 45 l S\n");
            pdfStream.append("BT\n/F1 7 Tf\n0.3 g\n55 30 Td (Digitally generated invoice by VR Technologies. Thank you for your business!) Tj\nET\n");
        }

        byte[] stream = pdfStream.toString().getBytes(StandardCharsets.UTF_8);
        String header = "%PDF-1.4\n";
        String obj1 = "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n";
        String obj2 = "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n";
        String obj3 = "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >> endobj\n";
        String obj4 = "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n";
        String obj5 = "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >> endobj\n";
        String obj6Prefix = "6 0 obj << /Length " + stream.length + " >> stream\n";
        String obj6Suffix = "\nendstream endobj\n";
        
        String[] parts = {header, obj1, obj2, obj3, obj4, obj5, obj6Prefix, new String(stream, StandardCharsets.US_ASCII), obj6Suffix};
        StringBuilder body = new StringBuilder();
        List<Integer> offsets = new ArrayList<>();
        body.append(header);
        for (int i = 1; i < parts.length; i++) {
            if (i <= 6) {
                offsets.add(body.length());
            }
            body.append(parts[i]);
        }
        int xref = body.length();
        body.append("xref\n0 7\n0000000000 65535 f \n");
        for (Integer item : offsets) {
            body.append(String.format("%010d 00000 n \n", item));
        }
        body.append("trailer << /Size 7 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        return body.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String generateInvoiceWordHtml(CustomerOrder order) {
        SiteSettings settings = siteSettingsRepository.findTopByOrderByIdAsc().orElseGet(SiteSettings::new);
        boolean gstEnabled = settings.isGstEnabled();
        BigDecimal gstRate = settings.getGstRate() == null ? BigDecimal.ZERO : settings.getGstRate();
        String defaultHsn = settings.getDefaultHsnCode();
        String layout = settings.getInvoiceLayout() != null ? settings.getInvoiceLayout().toUpperCase() : "MINIMAL";

        BigDecimal subtotal = order.getSubtotalAmount() == null ? BigDecimal.ZERO : order.getSubtotalAmount();
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        BigDecimal tax = order.getTaxAmount() == null ? BigDecimal.ZERO : order.getTaxAmount();
        BigDecimal delivery = order.getDeliveryCharge() == null ? BigDecimal.ZERO : order.getDeliveryCharge();
        BigDecimal grand = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();

        String companyState = defaultString(settings.getDefaultState(), "");
        String customerState = defaultString(order.getDeliveryState(), "");
        boolean interState = !companyState.isBlank() && !customerState.isBlank()
                && !companyState.equalsIgnoreCase(customerState);
        BigDecimal halfTax = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        String taxBreakdown = "";
        if (gstEnabled && tax.signum() > 0) {
            if (interState) {
                taxBreakdown = "<tr><td colspan='4' style='padding: 8px; text-align: right;'>IGST @ " + formatPercent(gstRate) + "</td><td style='padding: 8px; text-align: right;'>"
                        + formatCurrency(tax) + "</td></tr>";
            } else {
                taxBreakdown = "<tr><td colspan='4' style='padding: 8px; text-align: right;'>CGST @ " + formatPercent(gstRate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) + "</td><td style='padding: 8px; text-align: right;'>"
                        + formatCurrency(halfTax) + "</td></tr>"
                        + "<tr><td colspan='4' style='padding: 8px; text-align: right;'>SGST @ " + formatPercent(gstRate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) + "</td><td style='padding: 8px; text-align: right;'>"
                        + formatCurrency(halfTax) + "</td></tr>";
            }
        }

        String companyName = defaultString(settings.getCompanyName(), "VR Technologies");
        String companyAddressLine = defaultString(settings.getCompanyAddress(), "");
        String companyCityState = String.join(", ",
                List.of(
                        defaultString(settings.getDefaultCity(), ""),
                        defaultString(settings.getDefaultState(), ""),
                        defaultString(settings.getCompanyPincode(), "")
                ).stream().filter(s -> !s.isBlank()).toList());
        String gstinLine = settings.getGstNumber() != null && !settings.getGstNumber().isBlank()
                ? "GSTIN: " + escapeHtml(settings.getGstNumber()) : "";
        String panLine = settings.getCompanyPan() != null && !settings.getCompanyPan().isBlank()
                ? "PAN: " + escapeHtml(settings.getCompanyPan()) : "";
        String supportLine = String.join(" · ",
                List.of(
                        defaultString(settings.getSupportEmail(), ""),
                        defaultString(settings.getSupportPhone(), "")
                ).stream().filter(s -> !s.isBlank()).toList());
        String terms = settings.getInvoiceTerms() == null || settings.getInvoiceTerms().isBlank()
                ? "Thank you for shopping with " + escapeHtml(companyName) + ". This invoice was generated digitally and is valid without a signature."
                : escapeHtml(settings.getInvoiceTerms());

        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            BigDecimal lineTotal = item.getPriceAtTime().multiply(qty);
            String hsn = defaultString(item.getProduct().getHsnCode(), defaultString(defaultHsn, "-"));
            itemsHtml.append("""
                    <tr>
                      <td style="padding: 8px; border-bottom: 1px solid #e2e8f0;">%s</td>
                      <td style="padding: 8px; border-bottom: 1px solid #e2e8f0;">%s</td>
                      <td style="padding: 8px; border-bottom: 1px solid #e2e8f0; text-align: center;">%s</td>
                      <td style="padding: 8px; border-bottom: 1px solid #e2e8f0; text-align: right;">%s</td>
                      <td style="padding: 8px; border-bottom: 1px solid #e2e8f0; text-align: right;">%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(item.getProduct().getTitle()),
                    escapeHtml(hsn),
                    item.getQuantity(),
                    formatCurrency(item.getPriceAtTime()),
                    formatCurrency(lineTotal)
            ));
        }

        String bodyBg = "#ffffff";
        String cardBg = "#f8fbff";
        String textColor = "#0f172a";
        String accentColor = "#1e3a8a";
        String secondaryColor = "#475569";
        String borderStyle = "border: 1px solid rgba(30, 58, 138, 0.08);";
        String headerStyle = "background-color: #2F3699; color: #ffffff; padding: 24px; border-radius: 8px;";
        String thStyle = "background-color: #f1f5f9; color: #1e3a8a; font-weight: bold; text-align: left; padding: 10px;";

        if ("NEON".equals(layout)) {
            bodyBg = "#0b0f19";
            cardBg = "#131a2e";
            textColor = "#f8fafc";
            accentColor = "#00f0ff";
            secondaryColor = "#94a3b8";
            borderStyle = "border: 1px solid #00f0ff;";
            headerStyle = "background-color: #172540; color: #00f0ff; padding: 24px; border: 1px solid #00f0ff; border-radius: 8px;";
            thStyle = "background-color: #1e293b; color: #00f0ff; font-weight: bold; text-align: left; padding: 10px;";
        } else if ("MINIMAL".equals(layout)) {
            bodyBg = "#ffffff";
            cardBg = "#ffffff";
            textColor = "#000000";
            accentColor = "#000000";
            secondaryColor = "#333333";
            borderStyle = "border: 1px solid #000000;";
            headerStyle = "border-bottom: 2px solid #000000; padding: 20px 0;";
            thStyle = "border-bottom: 2px solid #000000; font-weight: bold; text-align: left; padding: 10px; color: #000000;";
        }

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <title>%s</title>
                  <style>
                    body { font-family: Arial, sans-serif; background-color: %s; color: %s; margin: 0; padding: 20px; }
                    .wrap { max-width: 800px; margin: 0 auto; padding: 20px; background-color: %s; }
                    .meta { font-size: 13px; color: %s; }
                    .h1-style { font-size: 28px; font-weight: bold; margin: 10px 0; color: %s; }
                    .h2-style { font-size: 16px; font-weight: bold; margin-bottom: 8px; color: %s; }
                    .grid-table { width: 100%%; margin-top: 20px; border-collapse: collapse; }
                    .card-cell { width: 50%%; padding: 15px; background-color: %s; %s vertical-align: top; border-radius: 6px; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div style="%s">
                      <table style="width: 100%%; border-collapse: collapse;">
                        <tr>
                          <td>
                            <div style="font-size: 11px; font-weight: bold; letter-spacing: 0.1em; color: %s; text-transform: uppercase;">%s</div>
                            <div class="h1-style">%s</div>
                            <div class="meta" style="color: %s;">Order: %s</div>
                            <div class="meta" style="color: %s;">Issued: %s</div>
                          </td>
                          <td style="text-align: right; vertical-align: top;">
                            <div class="h2-style" style="color: %s;">%s</div>
                            <div class="meta" style="color: %s;">%s</div>
                            <div class="meta" style="color: %s;">%s</div>
                            <div class="meta" style="color: %s;">%s</div>
                            %s
                            %s
                          </td>
                        </tr>
                      </table>
                    </div>

                    <table class="grid-table" style="margin-top: 20px;">
                      <tr>
                        <td class="card-cell">
                          <div class="h2-style" style="color: %s;">Bill To</div>
                          <div class="meta" style="color: %s; font-weight: bold;">%s</div>
                          <div class="meta" style="color: %s;">%s</div>
                          <div class="meta" style="color: %s;">%s</div>
                          <div class="meta" style="color: %s;">%s</div>
                        </td>
                        <td style="width: 20px;"></td>
                        <td class="card-cell">
                          <div class="h2-style" style="color: %s;">Fulfilment</div>
                          <div class="meta" style="color: %s;">Status: %s</div>
                          <div class="meta" style="color: %s;">Payment: %s</div>
                          <div class="meta" style="color: %s;">Method: %s</div>
                          <div class="meta" style="color: %s;">Store: %s</div>
                        </td>
                      </tr>
                    </table>

                    <table style="width: 100%%; border-collapse: collapse; margin-top: 30px;">
                      <thead>
                        <tr>
                          <th style="%s">Product</th>
                          <th style="%s">HSN/SAC</th>
                          <th style="%s text-align: center;">Qty</th>
                          <th style="%s text-align: right;">Unit Price</th>
                          <th style="%s text-align: right;">Line Total</th>
                        </tr>
                      </thead>
                      <tbody>
                        %s
                      </tbody>
                    </table>

                    <table style="width: 300px; margin-left: auto; margin-top: 20px; border-collapse: collapse;">
                      <tr>
                        <td style="padding: 6px; font-size: 13px; color: %s;">Subtotal</td>
                        <td style="padding: 6px; font-size: 13px; text-align: right; color: %s;">%s</td>
                      </tr>
                      %s
                      %s
                      %s
                      %s
                      <tr style="border-top: 2px solid %s;">
                        <td style="padding: 8px 6px; font-size: 16px; font-weight: bold; color: %s;">Grand Total</td>
                        <td style="padding: 8px 6px; font-size: 16px; font-weight: bold; text-align: right; color: %s;">%s</td>
                      </tr>
                    </table>

                    <div style="margin-top: 40px; border-top: 1px solid #e2e8f0; padding-top: 20px; font-size: 12px; color: %s; line-height: 1.5;">
                      <div style="font-weight: bold; margin-bottom: 5px; color: %s;">TERMS & CONDITIONS</div>
                      %s
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(order.getInvoiceNumber()),
                bodyBg, textColor, bodyBg, secondaryColor, accentColor, accentColor,
                cardBg, borderStyle,
                headerStyle,
                accentColor,
                gstEnabled ? "Tax Invoice" : "Invoice",
                escapeHtml(companyName),
                secondaryColor, escapeHtml(order.getOrderNumber()),
                secondaryColor, DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").format(order.getCreatedAt()),
                accentColor, escapeHtml(companyName),
                secondaryColor, escapeHtml(companyAddressLine.isBlank() ? "Refurbished systems marketplace" : companyAddressLine),
                secondaryColor, escapeHtml(companyCityState),
                secondaryColor, escapeHtml(supportLine),
                gstinLine.isBlank() ? "" : "<div style='color: " + accentColor + "; font-size: 11px; font-weight: bold;'>" + gstinLine + "</div>",
                panLine.isBlank() ? "" : "<div class='meta' style='color: " + secondaryColor + ";'>" + panLine + "</div>",
                accentColor, secondaryColor, escapeHtml(defaultString(order.getContactName(), "Customer")),
                secondaryColor, escapeHtml(defaultString(order.getContactPhone(), "-")),
                secondaryColor, escapeHtml(defaultString(order.getContactEmail(), "-")),
                secondaryColor, escapeHtml(defaultString(order.getDeliveryType() != null && order.getDeliveryType().name().equals("DELIVERY") ? order.getDeliveryAddress() : order.getStore() != null ? order.getStore().getAddress() : "-", "-")),
                accentColor, secondaryColor, escapeHtml(order.getStatus().name()),
                secondaryColor, escapeHtml(order.getPaymentStatus().name()),
                secondaryColor, escapeHtml(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "-"),
                secondaryColor, escapeHtml(order.getStore() != null ? order.getStore().getName() : "Not assigned"),
                thStyle, thStyle, thStyle, thStyle, thStyle,
                itemsHtml,
                secondaryColor, textColor, formatCurrency(subtotal),
                discount.signum() > 0 ? "<tr><td style='padding: 6px; font-size: 13px; color: " + secondaryColor + ";'>Discount</td><td style='padding: 6px; font-size: 13px; text-align: right; color: " + textColor + ";'>- " + formatCurrency(discount) + "</td></tr>" : "",
                taxBreakdown,
                delivery.signum() > 0 ? "<tr><td style='padding: 6px; font-size: 13px; color: " + secondaryColor + ";'>Delivery</td><td style='padding: 6px; font-size: 13px; text-align: right; color: " + textColor + ";'>" + formatCurrency(delivery) + "</td></tr>" : "",
                "",
                accentColor, accentColor, accentColor, formatCurrency(grand),
                secondaryColor, accentColor, escapeHtml(terms)
        );
    }

    public byte[] generateInvoiceWord(CustomerOrder order) {
        return generateInvoiceWordHtml(order).getBytes(StandardCharsets.UTF_8);
    }

    private String escapePdf(String value) {
        return defaultString(value, "").replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
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

    private BigDecimal calculateTax(SiteSettings settings, BigDecimal taxableAmount) {
        if (settings == null || !settings.isGstEnabled()) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = settings.getGstRate() == null ? BigDecimal.ZERO : settings.getGstRate();
        return taxableAmount
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
    }

    private BigDecimal calculateDeliveryCharge(SiteSettings settings, OrderRequest request, BigDecimal afterDiscount) {
        if (settings == null || request.getDeliveryType() == null || !request.getDeliveryType().name().equals("DELIVERY")) {
            return BigDecimal.ZERO;
        }
        BigDecimal threshold = settings.getFreeDeliveryThreshold();
        if (threshold != null && threshold.compareTo(BigDecimal.ZERO) > 0 && afterDiscount.compareTo(threshold) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal charge = settings.getStandardDeliveryCharge() == null ? BigDecimal.ZERO : settings.getStandardDeliveryCharge();
        BigDecimal stateCharge = resolveStateDeliveryCharge(settings.getStateDeliveryCharges(), request.getDeliveryState());
        if (stateCharge != null) {
            charge = stateCharge;
        }
        return charge.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveStateDeliveryCharge(String rules, String deliveryState) {
        String targetState = normalizeState(deliveryState);
        if (rules == null || rules.isBlank() || targetState == null) {
            return null;
        }
        String[] entries = rules.split("\\r?\\n|;");
        for (String entry : entries) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2 || !normalizeState(parts[0]).equals(targetState)) {
                continue;
            }
            try {
                return new BigDecimal(parts[1].trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String normalizeState(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sanitizeContactEmail(String value) {
        String email = trimToNull(value);
        if (email == null) {
            return null;
        }
        String normalized = email.toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        int atIndex = normalized.lastIndexOf('@');
        if (atIndex < 0 || atIndex == normalized.length() - 1) {
            return null;
        }
        String domain = normalized.substring(atIndex + 1);
        if (domain.endsWith(".local")) {
            return null;
        }
        return email.trim();
    }

    private String digitsOnly(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String digits = trimmed.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
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
