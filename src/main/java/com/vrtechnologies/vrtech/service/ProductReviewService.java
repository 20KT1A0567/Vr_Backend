package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.ProductReviewRequest;
import com.vrtechnologies.vrtech.dto.response.ProductReviewResponse;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.ProductImage;
import com.vrtechnologies.vrtech.entity.ProductReview;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.ReviewStatus;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.CustomerOrderRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.ProductReviewRepository;
import com.vrtechnologies.vrtech.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final UserContextService userContextService;
    private final CustomerOrderRepository customerOrderRepository;

    public ProductReviewService(
            ProductReviewRepository productReviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            PermissionService permissionService,
            UserContextService userContextService,
            CustomerOrderRepository customerOrderRepository
    ) {
        this.productReviewRepository = productReviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.userContextService = userContextService;
        this.customerOrderRepository = customerOrderRepository;
    }

    public List<ProductReviewResponse> getReviews(User admin) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        return productReviewRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(review -> canAccessReview(accessibleStoreIds, review))
                .map(this::toResponse)
                .toList();
    }

    public ProductReviewResponse saveReview(User admin, ProductReviewRequest request, Long id) {
        ProductReview review = id == null ? new ProductReview() : productReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        Product product = request.getProductId() == null ? null : productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (product != null) {
            requireProductAccess(admin, product);
        }
        if (id != null && !canAccessReview(permissionService.accessibleStoreIds(admin), review)) {
            throw new ResourceNotFoundException("Review not found");
        }

        review.setProduct(product);
        review.setUser(request.getUserId() == null ? null : userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
        review.setCustomerName(request.getCustomerName().trim());
        review.setCustomerEmail(normalize(request.getCustomerEmail()));
        review.setRating(request.getRating());
        review.setTitle(normalize(request.getTitle()));
        review.setComment(request.getComment().trim());
        review.setStatus(request.getStatus() == null ? ReviewStatus.PENDING : request.getStatus());
        review.setFeatured(request.isFeatured());
        review.setAdminNote(normalize(request.getAdminNote()));
        return toResponse(productReviewRepository.save(review));
    }

    public ProductReviewResponse submitCustomerReview(ProductReviewRequest request) {
        User user = userContextService.getCurrentUser();
        if (request.getProductId() == null) {
            throw new BadRequestException("Product is required");
        }
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        boolean purchased = customerOrderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .flatMap(order -> order.getItems().stream())
                .anyMatch(item -> item.getProduct().getId().equals(product.getId()));
        if (!purchased) {
            throw new BadRequestException("You can review this product after a delivered purchase");
        }
        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setCustomerName(user.getName());
        review.setCustomerEmail(user.getEmail());
        review.setRating(request.getRating());
        review.setTitle(normalize(request.getTitle()));
        review.setComment(request.getComment().trim());
        review.setStatus(ReviewStatus.PENDING);
        review.setFeatured(false);
        return toResponse(productReviewRepository.save(review));
    }

    public ProductReviewResponse updateStatus(User admin, Long id, ReviewStatus status) {
        ProductReview review = requireReview(admin, id);
        review.setStatus(status);
        if (status != ReviewStatus.APPROVED) {
            review.setFeatured(false);
        }
        return toResponse(productReviewRepository.save(review));
    }

    public ProductReviewResponse toggleFeatured(User admin, Long id) {
        ProductReview review = requireReview(admin, id);
        review.setFeatured(!review.isFeatured());
        if (review.isFeatured()) {
            review.setStatus(ReviewStatus.APPROVED);
        }
        return toResponse(productReviewRepository.save(review));
    }

    public void deleteReview(User admin, Long id) {
        ProductReview review = requireReview(admin, id);
        productReviewRepository.delete(review);
    }

    private ProductReview requireReview(User admin, Long id) {
        ProductReview review = productReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!canAccessReview(permissionService.accessibleStoreIds(admin), review)) {
            throw new ResourceNotFoundException("Review not found");
        }
        return review;
    }

    private boolean canAccessReview(List<Long> accessibleStoreIds, ProductReview review) {
        if (accessibleStoreIds == null || accessibleStoreIds.isEmpty()) {
            return true;
        }
        Product product = review.getProduct();
        return product != null && product.getStores().stream().map(Store::getId).anyMatch(accessibleStoreIds::contains);
    }

    private void requireProductAccess(User admin, Product product) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        if (accessibleStoreIds == null || accessibleStoreIds.isEmpty()) {
            return;
        }
        boolean allowed = product.getStores().stream().map(Store::getId).anyMatch(accessibleStoreIds::contains);
        if (!allowed) {
            throw new ResourceNotFoundException("Product not found");
        }
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        Product product = review.getProduct();
        String imageUrl = product == null ? null : product.getImages().stream()
                .min(Comparator.comparing(ProductImage::getSortOrder).thenComparing(ProductImage::getId))
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductReviewResponse.builder()
                .id(review.getId())
                .productId(product != null ? product.getId() : null)
                .productTitle(product != null ? product.getTitle() : null)
                .productImageUrl(imageUrl)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .customerName(review.getCustomerName())
                .customerEmail(review.getCustomerEmail())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .status(review.getStatus())
                .featured(review.isFeatured())
                .adminNote(review.getAdminNote())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
