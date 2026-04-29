package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.OrderRequest;
import com.vrtechnologies.vrtech.dto.response.OrderItemResponse;
import com.vrtechnologies.vrtech.dto.response.OrderResponse;
import com.vrtechnologies.vrtech.entity.CartItem;
import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.OrderItem;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.CartItemRepository;
import com.vrtechnologies.vrtech.repository.CustomerOrderRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final CartItemRepository cartItemRepository;
    private final StoreRepository storeRepository;
    private final UserContextService userContextService;
    private final ProductService productService;

    public OrderService(
            CustomerOrderRepository customerOrderRepository,
            CartItemRepository cartItemRepository,
            StoreRepository storeRepository,
            UserContextService userContextService,
            ProductService productService
    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.cartItemRepository = cartItemRepository;
        this.storeRepository = storeRepository;
        this.userContextService = userContextService;
        this.productService = productService;
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
        order.setContactName(request.getContactName());
        order.setContactPhone(request.getContactPhone());
        order.setContactEmail(request.getContactEmail());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setNotes(request.getNotes());

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
        cartItemRepository.deleteByUserId(user.getId());
        return toResponse(saved);
    }

    public List<OrderResponse> getMyOrders() {
        User user = userContextService.getCurrentUser();
        return customerOrderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> getAllOrders() {
        return customerOrderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getMyOrder(Long id) {
        User user = userContextService.getCurrentUser();
        CustomerOrder order = customerOrderRepository.findById(id)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.CANCELLED && status == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                releaseInventory(item.getProduct(), item.getQuantity());
            }
        } else if (order.getStatus() == OrderStatus.CANCELLED && status != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                reserveInventory(item.getProduct(), item.getQuantity());
            }
        }
        order.setStatus(status);
        return toResponse(customerOrderRepository.save(order));
    }

    public OrderResponse updatePaymentStatus(Long id, PaymentStatus paymentStatus) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setPaymentStatus(paymentStatus);
        return toResponse(customerOrderRepository.save(order));
    }

    private OrderResponse toResponse(CustomerOrder order) {
        return OrderResponse.builder()
                .id(order.getId())
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
}
