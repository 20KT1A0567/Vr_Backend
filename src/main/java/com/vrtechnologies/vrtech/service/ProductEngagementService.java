package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.PriceDropAlertRequest;
import com.vrtechnologies.vrtech.dto.request.RecentProductViewRequest;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.entity.PriceDropAlert;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.RecentlyViewedProduct;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.PriceDropAlertRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.RecentlyViewedProductRepository;
import com.vrtechnologies.vrtech.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductEngagementService {

    private final RecentlyViewedProductRepository recentlyViewedProductRepository;
    private final PriceDropAlertRepository priceDropAlertRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final NotificationService notificationService;

    public ProductEngagementService(
            RecentlyViewedProductRepository recentlyViewedProductRepository,
            PriceDropAlertRepository priceDropAlertRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            ProductService productService,
            NotificationService notificationService
    ) {
        this.recentlyViewedProductRepository = recentlyViewedProductRepository;
        this.priceDropAlertRepository = priceDropAlertRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productService = productService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void recordView(RecentProductViewRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        User user = currentUserOrNull();
        RecentlyViewedProduct row = user != null
                ? recentlyViewedProductRepository.findFirstByUserIdAndProductId(user.getId(), product.getId()).orElseGet(RecentlyViewedProduct::new)
                : recentlyViewedProductRepository.findFirstByAnonymousIdAndProductId(normalizeAnonymousId(request.getAnonymousId()), product.getId()).orElseGet(RecentlyViewedProduct::new);
        row.setUser(user);
        row.setAnonymousId(user == null ? normalizeAnonymousId(request.getAnonymousId()) : null);
        row.setProduct(product);
        recentlyViewedProductRepository.save(row);
    }

    public List<ProductResponse> recent(String anonymousId) {
        User user = currentUserOrNull();
        List<RecentlyViewedProduct> rows = user != null
                ? recentlyViewedProductRepository.findTop20ByUserIdOrderByUpdatedAtDescIdDesc(user.getId())
                : recentlyViewedProductRepository.findTop20ByAnonymousIdOrderByUpdatedAtDescIdDesc(normalizeAnonymousId(anonymousId));
        return rows.stream().map(row -> productService.toProductResponse(row.getProduct())).toList();
    }

    @Transactional
    public PriceDropAlert createPriceDropAlert(PriceDropAlertRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        PriceDropAlert alert = new PriceDropAlert();
        alert.setProduct(product);
        alert.setEmail(request.getEmail().trim());
        alert.setPhone(trimToNull(request.getPhone()));
        alert.setTargetPrice(request.getTargetPrice());
        return priceDropAlertRepository.save(alert);
    }

    public List<PriceDropAlert> latestPriceDropAlerts() {
        return priceDropAlertRepository.findTop100ByOrderByCreatedAtDescIdDesc();
    }

    @Scheduled(fixedDelayString = "${app.price-drop-alerts.worker.delay-ms:300000}")
    @Transactional
    public void dispatchDuePriceDropAlerts() {
        for (PriceDropAlert alert : priceDropAlertRepository.findTop200ByStatusOrderByCreatedAtAscIdAsc("WAITING")) {
            BigDecimal target = alert.getTargetPrice();
            BigDecimal current = alert.getProduct().getPrice();
            if (current == null || (target != null && current.compareTo(target) > 0)) {
                continue;
            }
            notificationService.log(
                    "PRICE_DROP",
                    "EMAIL",
                    alert.getEmail(),
                    "Price dropped: " + alert.getProduct().getTitle(),
                    "The price is now Rs. " + current + ".",
                    null
            );
            alert.setStatus("NOTIFIED");
            alert.setNotifiedAt(java.time.LocalDateTime.now());
            priceDropAlertRepository.save(alert);
        }
    }

    private User currentUserOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
    }

    private String normalizeAnonymousId(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BadRequestException("anonymousId is required for guest recent views");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
