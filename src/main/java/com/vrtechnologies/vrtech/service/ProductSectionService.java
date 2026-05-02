package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.ProductSectionItemRequest;
import com.vrtechnologies.vrtech.dto.request.ProductSectionRequest;
import com.vrtechnologies.vrtech.dto.response.HomeSectionResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.dto.response.ProductSectionItemResponse;
import com.vrtechnologies.vrtech.dto.response.ProductSectionResponse;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.ProductSection;
import com.vrtechnologies.vrtech.entity.ProductSectionProduct;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.ProductSectionSelectionMode;
import com.vrtechnologies.vrtech.entity.enums.ProductSectionType;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.OrderItemRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.ProductSectionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ProductSectionService {

    private static final int DEFAULT_SECTION_LIMIT = 8;

    private final ProductSectionRepository productSectionRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;

    public ProductSectionService(
            ProductSectionRepository productSectionRepository,
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository,
            ProductService productService
    ) {
        this.productSectionRepository = productSectionRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<HomeSectionResponse> getPublicHomeSections() {
        return productSectionRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .filter(this::isSectionActiveNow)
                .map(section -> HomeSectionResponse.builder()
                        .id(section.getId())
                        .title(section.getTitle())
                        .subtitle(section.getSubtitle())
                        .sectionType(section.getSectionType())
                        .displayOrder(section.getDisplayOrder())
                        .maxProducts(resolveSectionLimit(section.getMaxProducts()))
                        .startAt(section.getStartAt())
                        .endAt(section.getEndAt())
                        .products(resolvePublicProducts(section))
                        .build())
                .filter(section -> !section.getProducts().isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getBestSellers(int limit) {
        int normalizedLimit = normalizeLimit(limit);
        List<Long> productIds = orderItemRepository.findTopSellingProducts(
                        List.of(OrderStatus.CANCELLED, OrderStatus.REFUNDED),
                        PageRequest.of(0, normalizedLimit * 3)
                ).stream()
                .map(OrderItemRepository.ProductSalesProjection::getProductId)
                .filter(Objects::nonNull)
                .toList();

        List<ProductResponse> topSellingProducts = productService.getPublicProductsByIdsPreservingOrder(productIds).stream()
                .limit(normalizedLimit)
                .map(productService::toProductResponse)
                .toList();

        List<ProductResponse> curatedBestSellerProducts = productService.getFlaggedBestSellers(normalizedLimit);
        List<ProductResponse> bestSellerProducts = mergeProducts(topSellingProducts, curatedBestSellerProducts, normalizedLimit);

        if (!bestSellerProducts.isEmpty()) {
            return bestSellerProducts;
        }

        return productService.getFeaturedProducts(normalizedLimit);
    }

    @Transactional(readOnly = true)
    public List<ProductSectionResponse> getAllSections() {
        return productSectionRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toSectionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductSectionResponse getSection(Long id) {
        return toSectionResponse(productSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product section not found")));
    }

    @Transactional
    public ProductSectionResponse saveSection(ProductSectionRequest request, Long id) {
        validateSectionRequest(request);

        ProductSection section = id == null ? new ProductSection() : productSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product section not found"));
        section.setTitle(request.getTitle().trim());
        section.setSubtitle(normalizeString(request.getSubtitle()));
        section.setSectionType(request.getSectionType());
        section.setSelectionMode(request.getSelectionMode() == null ? ProductSectionSelectionMode.AUTOMATIC : request.getSelectionMode());
        section.setDisplayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder());
        section.setActive(request.getActive() == null || request.getActive());
        section.setStartAt(request.getStartAt());
        section.setEndAt(request.getEndAt());
        section.setMaxProducts(request.getMaxProducts() == null ? DEFAULT_SECTION_LIMIT : request.getMaxProducts());

        syncSectionProducts(section, resolveProductRequests(request));
        return toSectionResponse(productSectionRepository.save(section));
    }

    @Transactional
    public void deleteSection(Long id) {
        ProductSection section = productSectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product section not found"));
        productSectionRepository.delete(section);
    }

    private ProductSectionResponse toSectionResponse(ProductSection section) {
        List<ProductSectionItemResponse> manualProducts = section.getProducts().stream()
                .sorted(Comparator.comparing(ProductSectionProduct::getDisplayOrder).thenComparing(ProductSectionProduct::getId))
                .map(item -> ProductSectionItemResponse.builder()
                        .id(item.getId())
                        .displayOrder(item.getDisplayOrder())
                        .product(productService.toProductResponse(item.getProduct()))
                        .build())
                .toList();

        return ProductSectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .subtitle(section.getSubtitle())
                .sectionType(section.getSectionType())
                .selectionMode(section.getSelectionMode())
                .displayOrder(section.getDisplayOrder())
                .active(section.isActive())
                .startAt(section.getStartAt())
                .endAt(section.getEndAt())
                .maxProducts(section.getMaxProducts())
                .products(manualProducts)
                .resolvedProducts(resolvePublicProducts(section))
                .build();
    }

    private List<ProductResponse> resolvePublicProducts(ProductSection section) {
        int limit = resolveSectionLimit(section.getMaxProducts());
        List<ProductResponse> manualProducts = resolveManualProducts(section);
        List<ProductResponse> automaticProducts = resolveAutomaticProducts(section.getSectionType(), limit);

        return switch (section.getSelectionMode()) {
            case MANUAL -> manualProducts.stream().limit(limit).toList();
            case HYBRID -> mergeProducts(manualProducts, automaticProducts, limit);
            case AUTOMATIC -> automaticProducts.stream().limit(limit).toList();
        };
    }

    private List<ProductResponse> resolveManualProducts(ProductSection section) {
        List<Long> productIds = section.getProducts().stream()
                .sorted(Comparator.comparing(ProductSectionProduct::getDisplayOrder).thenComparing(ProductSectionProduct::getId))
                .map(item -> item.getProduct().getId())
                .toList();
        return productService.getPublicProductsByIdsPreservingOrder(productIds).stream()
                .map(productService::toProductResponse)
                .toList();
    }

    private List<ProductResponse> resolveAutomaticProducts(ProductSectionType sectionType, int limit) {
        return switch (sectionType) {
            case BEST_SELLERS -> getBestSellers(limit);
            case TODAYS_DEALS -> productService.getTodaysDeals(limit);
            case FEATURED_PRODUCTS -> productService.getFeaturedProducts(limit);
            case NEW_ARRIVALS -> productService.getNewArrivals(limit);
            case LOW_PRICE_DEALS -> getLowPriceDeals(limit);
            case TRENDING_PRODUCTS -> getTrendingProducts(limit);
            case TOP_RATED -> getTopRatedFallback(limit);
            case RECOMMENDED_PRODUCTS -> getRecommendedProducts(limit);
        };
    }

    private List<ProductResponse> getLowPriceDeals(int limit) {
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "price")).stream()
                .filter(productService::isPubliclyVisible)
                .filter(product -> (product.getDiscountPercent() != null && product.getDiscountPercent() > 0)
                        || (product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(product.getPrice()) > 0))
                .limit(normalizeLimit(limit))
                .map(productService::toProductResponse)
                .toList();
    }

    private List<ProductResponse> getTrendingProducts(int limit) {
        List<ProductResponse> merged = mergeProducts(
                getBestSellers(limit),
                productService.getTodaysDeals(limit),
                normalizeLimit(limit)
        );
        if (!merged.isEmpty()) {
            return merged;
        }
        return productService.getNewArrivals(limit);
    }

    private List<ProductResponse> getTopRatedFallback(int limit) {
        return mergeProducts(
                getBestSellers(limit),
                productService.getFeaturedProducts(limit),
                normalizeLimit(limit)
        );
    }

    private List<ProductResponse> getRecommendedProducts(int limit) {
        return mergeProducts(
                productService.getFeaturedProducts(limit),
                productService.getNewArrivals(limit),
                normalizeLimit(limit)
        );
    }

    private List<ProductResponse> mergeProducts(List<ProductResponse> primary, List<ProductResponse> secondary, int limit) {
        Map<Long, ProductResponse> merged = new LinkedHashMap<>();
        for (ProductResponse product : primary) {
            merged.putIfAbsent(product.getId(), product);
            if (merged.size() >= limit) {
                return new ArrayList<>(merged.values());
            }
        }
        for (ProductResponse product : secondary) {
            merged.putIfAbsent(product.getId(), product);
            if (merged.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(merged.values());
    }

    private List<ProductSectionItemRequest> resolveProductRequests(ProductSectionRequest request) {
        if (request.getProducts() != null && !request.getProducts().isEmpty()) {
            return request.getProducts();
        }
        if (request.getProductIds() == null || request.getProductIds().isEmpty()) {
            return request.getProducts();
        }

        List<ProductSectionItemRequest> resolvedRequests = new ArrayList<>();
        for (int index = 0; index < request.getProductIds().size(); index++) {
            ProductSectionItemRequest itemRequest = new ProductSectionItemRequest();
            itemRequest.setProductId(request.getProductIds().get(index));
            itemRequest.setDisplayOrder(index);
            resolvedRequests.add(itemRequest);
        }
        return resolvedRequests;
    }

    private void syncSectionProducts(ProductSection section, List<ProductSectionItemRequest> requests) {
        section.getProducts().clear();
        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<Long> requestedProductIds = requests.stream()
                .map(ProductSectionItemRequest::getProductId)
                .toList();
        Map<Long, Product> productsById = productRepository.findAllById(requestedProductIds).stream()
                .collect(LinkedHashMap::new, (map, product) -> map.put(product.getId(), product), LinkedHashMap::putAll);

        if (productsById.size() != new LinkedHashSet<>(requestedProductIds).size()) {
            throw new ResourceNotFoundException("One or more products were not found");
        }

        Set<Long> seenProductIds = new LinkedHashSet<>();
        for (int index = 0; index < requests.size(); index++) {
            ProductSectionItemRequest itemRequest = requests.get(index);
            if (!seenProductIds.add(itemRequest.getProductId())) {
                throw new BadRequestException("Duplicate products are not allowed in a product section");
            }

            ProductSectionProduct sectionProduct = new ProductSectionProduct();
            sectionProduct.setSection(section);
            sectionProduct.setProduct(productsById.get(itemRequest.getProductId()));
            sectionProduct.setDisplayOrder(itemRequest.getDisplayOrder() == null ? index : itemRequest.getDisplayOrder());
            section.getProducts().add(sectionProduct);
        }
    }

    private void validateSectionRequest(ProductSectionRequest request) {
        if (request.getStartAt() != null && request.getEndAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
            throw new BadRequestException("Section end date must be after start date");
        }
        if (request.getDisplayOrder() != null && request.getDisplayOrder() < 0) {
            throw new BadRequestException("Section display order cannot be negative");
        }
        if (request.getMaxProducts() != null && request.getMaxProducts() < 1) {
            throw new BadRequestException("Section max products must be at least 1");
        }
    }

    private boolean isSectionActiveNow(ProductSection section) {
        LocalDateTime now = LocalDateTime.now();
        if (!section.isActive()) {
            return false;
        }
        if (section.getStartAt() != null && now.isBefore(section.getStartAt())) {
            return false;
        }
        if (section.getEndAt() != null && now.isAfter(section.getEndAt())) {
            return false;
        }
        return true;
    }

    private int resolveSectionLimit(Integer maxProducts) {
        return normalizeLimit(maxProducts == null ? DEFAULT_SECTION_LIMIT : maxProducts);
    }

    private int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 24);
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
