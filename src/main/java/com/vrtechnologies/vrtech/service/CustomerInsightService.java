package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.response.Customer360Response;
import com.vrtechnologies.vrtech.entity.BackInStockRequest;
import com.vrtechnologies.vrtech.entity.CartItem;
import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.Enquiry;
import com.vrtechnologies.vrtech.entity.OrderItem;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.ProductImage;
import com.vrtechnologies.vrtech.entity.ReturnRequest;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.UserAddress;
import com.vrtechnologies.vrtech.entity.WishlistItem;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import com.vrtechnologies.vrtech.entity.enums.ReturnRequestStatus;
import com.vrtechnologies.vrtech.entity.enums.Role;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.BackInStockRequestRepository;
import com.vrtechnologies.vrtech.repository.CartItemRepository;
import com.vrtechnologies.vrtech.repository.CustomerOrderRepository;
import com.vrtechnologies.vrtech.repository.EnquiryRepository;
import com.vrtechnologies.vrtech.repository.ReturnRequestRepository;
import com.vrtechnologies.vrtech.repository.UserAddressRepository;
import com.vrtechnologies.vrtech.repository.UserRepository;
import com.vrtechnologies.vrtech.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CustomerInsightService {

    private static final int RECENT_ORDERS_LIMIT = 25;
    private static final Set<OrderStatus> COMPLETED_STATUSES = Set.of(OrderStatus.DELIVERED);

    private final UserRepository userRepository;
    private final CustomerOrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final EnquiryRepository enquiryRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final BackInStockRequestRepository backInStockRequestRepository;
    private final UserAddressRepository userAddressRepository;

    public CustomerInsightService(
            UserRepository userRepository,
            CustomerOrderRepository orderRepository,
            CartItemRepository cartItemRepository,
            WishlistItemRepository wishlistItemRepository,
            EnquiryRepository enquiryRepository,
            ReturnRequestRepository returnRequestRepository,
            BackInStockRequestRepository backInStockRequestRepository,
            UserAddressRepository userAddressRepository
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.enquiryRepository = enquiryRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.backInStockRequestRepository = backInStockRequestRepository;
        this.userAddressRepository = userAddressRepository;
    }

    @Transactional(readOnly = true)
    public Customer360Response getCustomer360(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (customer.getRole() != Role.USER) {
            throw new ResourceNotFoundException("Customer not found");
        }

        List<CustomerOrder> allOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(customer.getId());
        List<CartItem> cart = cartItemRepository.findByUserIdOrderByIdAsc(customer.getId());
        List<WishlistItem> wishlist = wishlistItemRepository.findByUserIdOrderByAddedAtDesc(customer.getId());
        List<ReturnRequest> returns = returnRequestRepository.findByUserIdOrderByCreatedAtDescIdDesc(customer.getId());
        List<UserAddress> addresses = userAddressRepository
                .findByUserIdOrderByDefaultAddressDescUpdatedAtDescIdDesc(customer.getId());

        List<Enquiry> enquiries = mergedEnquiries(customer);
        List<BackInStockRequest> backInStock = customer.getEmail() == null || customer.getEmail().isBlank()
                ? List.of()
                : backInStockRequestRepository.findByEmailIgnoreCaseOrderByCreatedAtDescIdDesc(customer.getEmail());

        return Customer360Response.builder()
                .profile(toProfile(customer))
                .summary(buildSummary(allOrders, cart, wishlist, enquiries, returns, backInStock))
                .recentOrders(toOrderItems(allOrders))
                .cart(toCartLines(cart))
                .wishlist(toWishlistLines(wishlist))
                .enquiries(toEnquiryLines(enquiries))
                .returns(toReturnLines(returns))
                .backInStock(toBackInStockLines(backInStock))
                .addresses(toAddressLines(addresses))
                .build();
    }

    private List<Enquiry> mergedEnquiries(User customer) {
        Map<Long, Enquiry> merged = new LinkedHashMap<>();
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            for (Enquiry enquiry : enquiryRepository
                    .findByEmailIgnoreCaseOrderByCreatedAtDescIdDesc(customer.getEmail())) {
                merged.put(enquiry.getId(), enquiry);
            }
        }
        if (customer.getPhone() != null && !customer.getPhone().isBlank()) {
            for (Enquiry enquiry : enquiryRepository
                    .findByPhoneOrderByCreatedAtDescIdDesc(customer.getPhone())) {
                merged.putIfAbsent(enquiry.getId(), enquiry);
            }
        }
        List<Enquiry> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(Enquiry::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private Customer360Response.Profile toProfile(User user) {
        return Customer360Response.Profile.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .active(user.isActive())
                .profileImageUrl(user.getProfileImageUrl())
                .registeredAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private Customer360Response.Summary buildSummary(
            List<CustomerOrder> orders,
            List<CartItem> cart,
            List<WishlistItem> wishlist,
            List<Enquiry> enquiries,
            List<ReturnRequest> returns,
            List<BackInStockRequest> backInStock
    ) {
        BigDecimal lifetimeSpend = BigDecimal.ZERO;
        long completedCount = 0;
        long cancelledCount = 0;
        for (CustomerOrder order : orders) {
            if (order.getPaymentStatus() == PaymentStatus.PAID && order.getTotalAmount() != null) {
                lifetimeSpend = lifetimeSpend.add(order.getTotalAmount());
            }
            if (COMPLETED_STATUSES.contains(order.getStatus())) {
                completedCount++;
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                cancelledCount++;
            }
        }

        BigDecimal aov = completedCount > 0
                ? lifetimeSpend.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long openEnquiries = enquiries.stream()
                .filter(e -> e.getStatus() != null && !"RESOLVED".equalsIgnoreCase(e.getStatus().name()))
                .count();
        long openReturns = returns.stream()
                .filter(r -> r.getStatus() == ReturnRequestStatus.REQUESTED
                        || r.getStatus() == ReturnRequestStatus.APPROVED)
                .count();
        long openBis = backInStock.stream()
                .filter(b -> b.getStatus() == null || "WAITING".equalsIgnoreCase(b.getStatus()))
                .count();

        return Customer360Response.Summary.builder()
                .totalOrders(orders.size())
                .completedOrders(completedCount)
                .cancelledOrders(cancelledCount)
                .lifetimeSpend(lifetimeSpend)
                .averageOrderValue(aov)
                .firstOrderAt(orders.isEmpty() ? null : orders.get(orders.size() - 1).getCreatedAt())
                .lastOrderAt(orders.isEmpty() ? null : orders.get(0).getCreatedAt())
                .openEnquiries(openEnquiries)
                .openReturns(openReturns)
                .cartItemCount(cart.size())
                .wishlistItemCount(wishlist.size())
                .backInStockSubscriptions(openBis)
                .build();
    }

    private List<Customer360Response.OrderItem> toOrderItems(List<CustomerOrder> orders) {
        List<Customer360Response.OrderItem> result = new ArrayList<>(Math.min(orders.size(), RECENT_ORDERS_LIMIT));
        for (int i = 0; i < orders.size() && i < RECENT_ORDERS_LIMIT; i++) {
            CustomerOrder o = orders.get(i);
            int itemCount = 0;
            if (o.getItems() != null) {
                for (OrderItem item : o.getItems()) {
                    itemCount += item.getQuantity() == null ? 0 : item.getQuantity();
                }
            }
            result.add(Customer360Response.OrderItem.builder()
                    .id(o.getId())
                    .orderNumber(o.getOrderNumber())
                    .status(o.getStatus())
                    .paymentStatus(o.getPaymentStatus())
                    .totalAmount(o.getTotalAmount())
                    .itemCount(itemCount)
                    .storeName(o.getStore() != null ? o.getStore().getName() : null)
                    .placedAt(o.getCreatedAt())
                    .paidAt(o.getPaidAt())
                    .deliveredAt(o.getDeliveredAt())
                    .cancelledAt(o.getCancelledAt())
                    .build());
        }
        return result;
    }

    private List<Customer360Response.CartLine> toCartLines(List<CartItem> cart) {
        List<Customer360Response.CartLine> result = new ArrayList<>(cart.size());
        for (CartItem item : cart) {
            Product product = item.getProduct();
            BigDecimal price = product != null && product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            result.add(Customer360Response.CartLine.builder()
                    .id(item.getId())
                    .productId(product != null ? product.getId() : null)
                    .productTitle(product != null ? product.getTitle() : null)
                    .productImageUrl(primaryImageUrl(product))
                    .price(price)
                    .quantity(quantity)
                    .lineTotal(price.multiply(BigDecimal.valueOf(quantity)))
                    .addedAt(null)
                    .build());
        }
        return result;
    }

    private List<Customer360Response.WishlistLine> toWishlistLines(List<WishlistItem> wishlist) {
        List<Customer360Response.WishlistLine> result = new ArrayList<>(wishlist.size());
        for (WishlistItem item : wishlist) {
            Product product = item.getProduct();
            result.add(Customer360Response.WishlistLine.builder()
                    .id(item.getId())
                    .productId(product != null ? product.getId() : null)
                    .productTitle(product != null ? product.getTitle() : null)
                    .productImageUrl(primaryImageUrl(product))
                    .price(product != null ? product.getPrice() : null)
                    .inStock(product != null && product.isAvailable()
                            && product.getStockQuantity() != null && product.getStockQuantity() > 0)
                    .addedAt(item.getAddedAt())
                    .build());
        }
        return result;
    }

    private List<Customer360Response.EnquiryLine> toEnquiryLines(List<Enquiry> enquiries) {
        List<Customer360Response.EnquiryLine> result = new ArrayList<>(enquiries.size());
        for (Enquiry enquiry : enquiries) {
            Product product = enquiry.getProduct();
            result.add(Customer360Response.EnquiryLine.builder()
                    .id(enquiry.getId())
                    .enquiryType(enquiry.getEnquiryType())
                    .status(enquiry.getStatus() != null ? enquiry.getStatus().name() : null)
                    .message(enquiry.getMessage())
                    .productId(product != null ? product.getId() : null)
                    .productTitle(product != null ? product.getTitle() : null)
                    .createdAt(enquiry.getCreatedAt())
                    .build());
        }
        return result;
    }

    private List<Customer360Response.ReturnLine> toReturnLines(List<ReturnRequest> returns) {
        List<Customer360Response.ReturnLine> result = new ArrayList<>(returns.size());
        for (ReturnRequest r : returns) {
            CustomerOrder order = r.getOrder();
            result.add(Customer360Response.ReturnLine.builder()
                    .id(r.getId())
                    .orderId(order != null ? order.getId() : null)
                    .orderNumber(order != null ? order.getOrderNumber() : null)
                    .status(r.getStatus())
                    .reason(r.getReason())
                    .createdAt(r.getCreatedAt())
                    .resolvedAt(r.getResolvedAt())
                    .build());
        }
        return result;
    }

    private List<Customer360Response.BackInStockLine> toBackInStockLines(List<BackInStockRequest> requests) {
        List<Customer360Response.BackInStockLine> result = new ArrayList<>(requests.size());
        for (BackInStockRequest req : requests) {
            Product product = req.getProduct();
            result.add(Customer360Response.BackInStockLine.builder()
                    .id(req.getId())
                    .productId(product != null ? product.getId() : null)
                    .productTitle(product != null ? product.getTitle() : null)
                    .status(req.getStatus())
                    .createdAt(req.getCreatedAt())
                    .build());
        }
        return result;
    }

    private List<Customer360Response.AddressLine> toAddressLines(List<UserAddress> addresses) {
        List<Customer360Response.AddressLine> result = new ArrayList<>(addresses.size());
        for (UserAddress addr : addresses) {
            result.add(Customer360Response.AddressLine.builder()
                    .id(addr.getId())
                    .label(addr.getLabel())
                    .fullName(addr.getContactName())
                    .phone(addr.getContactPhone())
                    .addressLine1(addr.getAddress())
                    .addressLine2(null)
                    .city(addr.getCity())
                    .state(addr.getState())
                    .pincode(addr.getPostalCode())
                    .defaultAddress(addr.isDefaultAddress())
                    .build());
        }
        return result;
    }

    private String primaryImageUrl(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        ProductImage primary = null;
        for (ProductImage image : product.getImages()) {
            if (image.isPrimaryImage()) {
                primary = image;
                break;
            }
        }
        if (primary == null) {
            primary = product.getImages().iterator().next();
        }
        return primary != null ? primary.getImageUrl() : null;
    }
}
