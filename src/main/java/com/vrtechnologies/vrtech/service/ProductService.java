package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.ProductBulkActionRequest;
import com.vrtechnologies.vrtech.dto.request.ProductImageRequest;
import com.vrtechnologies.vrtech.dto.request.ProductRequest;
import com.vrtechnologies.vrtech.dto.response.MediaUploadResponse;
import com.vrtechnologies.vrtech.dto.response.ProductImageResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.dto.response.StoreSummaryResponse;
import com.vrtechnologies.vrtech.entity.Brand;
import com.vrtechnologies.vrtech.entity.Category;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.ProductImage;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.ProductCondition;
import com.vrtechnologies.vrtech.entity.enums.ProductBulkActionType;
import com.vrtechnologies.vrtech.entity.enums.ProductStatus;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.BrandRepository;
import com.vrtechnologies.vrtech.repository.CategoryRepository;
import com.vrtechnologies.vrtech.repository.ProductImageRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ProductService {

    private static final int MIN_PRODUCT_IMAGES = 1;
    private static final int MAX_PRODUCT_IMAGES = 20;

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final ProductImageRepository productImageRepository;
    private final CloudinaryService cloudinaryService;
    private final PermissionService permissionService;

    public ProductService(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            StoreRepository storeRepository,
            ProductImageRepository productImageRepository,
            CloudinaryService cloudinaryService,
            PermissionService permissionService
    ) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.productImageRepository = productImageRepository;
        this.cloudinaryService = cloudinaryService;
        this.permissionService = permissionService;
    }

    public List<ProductResponse> getPublicProducts(
            String query,
            List<Long> brandIds,
            List<Long> categoryIds,
            Long storeId,
            List<Integer> ramOptions,
            List<Integer> storageOptions,
            List<String> processorOptions,
            List<ProductCondition> conditions,
            Boolean inStock,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        Specification<Product> specification = Specification.where(publicVisibility())
                .and(matchesQuery(query))
                .and(byBrands(brandIds))
                .and(byCategories(categoryIds))
                .and(byStore(storeId))
                .and(byRamOptions(ramOptions))
                .and(byStorageOptions(storageOptions))
                .and(byProcessors(processorOptions))
                .and(byConditions(conditions))
                .and(byStockAvailability(inStock))
                .and(minPrice(minPrice))
                .and(maxPrice(maxPrice));

        return productRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    public List<ProductResponse> getAllProductsForAdmin(User admin) {
        return getAllProductsForAdmin(admin, null, null, null, null, null, null, null, null, null, null, null);
    }

    public List<ProductResponse> getAllProductsForAdmin(
            User admin,
            String query,
            List<Long> brandIds,
            List<Long> categoryIds,
            List<Long> storeIds,
            List<String> stockStates,
            Boolean available,
            Boolean featured,
            Boolean bestSeller,
            Boolean todayDeal,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        Specification<Product> specification = Specification.where(accessibleStores(accessibleStoreIds))
                .and(matchesAdminQuery(query))
                .and(byBrands(brandIds))
                .and(byCategories(categoryIds))
                .and(byStores(storeIds))
                .and(byStockStates(stockStates))
                .and(byAvailability(available))
                .and(byFeatured(featured))
                .and(byBestSeller(bestSeller))
                .and(byTodayDeal(todayDeal))
                .and(minPrice(minPrice))
                .and(maxPrice(maxPrice));

        return productRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(this::toProductResponse)
                .toList();
    }

    public ProductResponse getAdminProduct(User admin, Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        requireProductAccess(admin, product);
        return toProductResponse(product);
    }

    public List<ProductResponse> getFeaturedProducts() {
        return getFeaturedProducts(8);
    }

    public List<ProductResponse> getFeaturedProducts(int limit) {
        return listPublicProductsSorted(Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("updatedAt"))).stream()
                .filter(Product::isFeatured)
                .limit(normalizeLimit(limit))
                .map(this::toProductResponse)
                .toList();
    }

    public List<ProductResponse> getFlaggedBestSellers(int limit) {
        return listPublicProductsSorted(Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("updatedAt"))).stream()
                .filter(Product::isBestSellerEnabled)
                .sorted(Comparator
                        .comparingInt(Product::getResolvedDisplayOrder)
                        .thenComparing(Product::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(normalizeLimit(limit))
                .map(this::toProductResponse)
                .toList();
    }

    public List<ProductResponse> getNewArrivals(int limit) {
        return listPublicProductsSorted(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .limit(normalizeLimit(limit))
                .map(this::toProductResponse)
                .toList();
    }

    public List<ProductResponse> getTodaysDeals(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return listPublicProductsSorted(Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("updatedAt"))).stream()
                .map(this::toProductResponse)
                .filter(product -> isDealActive(product, now))
                .sorted(Comparator
                        .comparingInt((ProductResponse product) -> product.getDisplayOrder() == null ? 0 : product.getDisplayOrder())
                        .thenComparing(this::discountValue, Comparator.reverseOrder())
                        .thenComparing(ProductResponse::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(normalizeLimit(limit))
                .toList();
    }

    public List<Product> getPublicProductsByIdsPreservingOrder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productsById = productRepository.findByIdIn(ids).stream()
                .filter(this::isPubliclyVisible)
                .collect(LinkedHashMap::new, (map, product) -> map.put(product.getId(), product), LinkedHashMap::putAll);

        return ids.stream()
                .map(productsById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public ProductResponse getProduct(Long id, boolean adminView) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!adminView && !isPubliclyVisible(product)) {
            throw new ResourceNotFoundException("Product not found");
        }

        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(User admin, ProductRequest request) {
        validateImageCount(request.getImages() == null ? 0 : request.getImages().size());
        validateRequestedStoreAccess(admin, request.getStoreIds());
        Product product = new Product();
        applyRequest(product, request);
        attachImages(product, request.getImages());
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(User admin, Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        requireProductAccess(admin, product);
        validateRequestedStoreAccess(admin, request.getStoreIds());
        applyRequest(product, request);
        validateImageCount(product.getImages().size());
        return toProductResponse(productRepository.save(product));
    }

    public void deleteProduct(User admin, Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        requireProductAccess(admin, product);
        productRepository.delete(product);
    }

    @Transactional
    public void applyBulkAction(User admin, ProductBulkActionRequest request) {
        ProductBulkActionType action = request.getAction();
        if (action == null) {
            throw new BadRequestException("Bulk action is required");
        }

        List<Long> requestedIds = request.getProductIds() == null ? List.of() : request.getProductIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (requestedIds.isEmpty()) {
            throw new BadRequestException("Select at least 1 product");
        }

        List<Product> products = productRepository.findAllById(requestedIds);
        if (products.size() != requestedIds.size()) {
            throw new ResourceNotFoundException("One or more products were not found");
        }

        for (Product product : products) {
            requireProductAccess(admin, product);
        }

        switch (action) {
            case DELETE -> productRepository.deleteAll(products);
            case SET_VISIBILITY -> {
                if (request.getVisible() == null) {
                    throw new BadRequestException("Visibility value is required");
                }
                products.forEach(product -> product.setAvailable(request.getVisible()));
                productRepository.saveAll(products);
            }
            case ASSIGN_CATEGORY -> {
                if (request.getCategoryId() == null) {
                    throw new BadRequestException("Category is required");
                }
                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
                products.forEach(product -> product.setCategory(category));
                productRepository.saveAll(products);
            }
            case ADJUST_PRICE_PERCENT -> {
                if (request.getPriceAdjustmentPercent() == null) {
                    throw new BadRequestException("Price adjustment percentage is required");
                }
                products.forEach(product -> applyPriceAdjustment(product, request.getPriceAdjustmentPercent()));
                productRepository.saveAll(products);
            }
            case SET_FEATURED -> {
                if (request.getEnabled() == null) {
                    throw new BadRequestException("Enabled value is required");
                }
                products.forEach(product -> product.setFeatured(request.getEnabled()));
                productRepository.saveAll(products);
            }
            case SET_TODAY_DEAL -> {
                if (request.getEnabled() == null) {
                    throw new BadRequestException("Enabled value is required");
                }
                products.forEach(product -> applyTodayDealState(product, request.getEnabled()));
                productRepository.saveAll(products);
            }
            case SET_BEST_SELLER -> {
                if (request.getEnabled() == null) {
                    throw new BadRequestException("Enabled value is required");
                }
                products.forEach(product -> product.setBestSeller(request.getEnabled()));
                productRepository.saveAll(products);
            }
        }
    }

    @Transactional
    public ProductResponse duplicateProduct(User admin, Long id) {
        Product source = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        requireProductAccess(admin, source);

        Product copy = new Product();
        copy.setTitle(source.getTitle() + " (Copy)");
        copy.setBrand(source.getBrand());
        copy.setCategory(source.getCategory());
        copy.setStores(new LinkedHashSet<>(source.getStores()));
        copy.setModelNumber(source.getModelNumber());
        copy.setProcessor(source.getProcessor());
        copy.setProcessorGeneration(source.getProcessorGeneration());
        copy.setRamGb(source.getRamGb());
        copy.setStorageGb(source.getStorageGb());
        copy.setStorageType(source.getStorageType());
        copy.setDisplaySize(source.getDisplaySize());
        copy.setDisplayType(source.getDisplayType());
        copy.setOs(source.getOs());
        copy.setGraphicsCard(source.getGraphicsCard());
        copy.setBattery(source.getBattery());
        copy.setWeight(source.getWeight());
        copy.setWarrantyMonths(source.getWarrantyMonths());
        copy.setWarrantySummary(source.getWarrantySummary());
        copy.setReturnDays(source.getReturnDays());
        copy.setSku(null);
        copy.setSerialNumber(null);
        copy.setProductCondition(source.getProductCondition());
        copy.setProductStatus(source.getProductStatus());
        copy.setPrice(source.getPrice());
        copy.setOriginalPrice(source.getOriginalPrice());
        copy.setDiscountPercent(source.getDiscountPercent());
        copy.setStockQuantity(source.getStockQuantity());
        copy.setAvailable(false);
        copy.setFeatured(false);
        copy.setBestSeller(false);
        copy.setTodayDeal(false);
        copy.setDealStartDate(null);
        copy.setDealEndDate(null);
        copy.setDisplayOrder(source.getDisplayOrder());
        copy.setVideoUrl(source.getVideoUrl());
        copy.setSeoTitle(source.getSeoTitle());
        copy.setSeoDescription(source.getSeoDescription());
        copy.setSeoKeywords(source.getSeoKeywords());
        copy.setLowStockThreshold(source.getLowStockThreshold());
        copy.setDescription(source.getDescription());
        copy.setCustomAttributes(source.getCustomAttributes() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source.getCustomAttributes()));

        source.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getSortOrder).thenComparing(ProductImage::getId))
                .forEach(sourceImage -> {
                    ProductImage image = new ProductImage();
                    image.setProduct(copy);
                    image.setImageUrl(sourceImage.getImageUrl());
                    image.setPublicId(null);
                    image.setSortOrder(sourceImage.getSortOrder());
                    image.setPrimaryImage(sourceImage.isPrimaryImage());
                    copy.getImages().add(image);
                });

        return toProductResponse(productRepository.save(copy));
    }

    @Transactional
    public ProductResponse uploadProductImage(User admin, Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        requireProductAccess(admin, product);

        if (product.getImages().size() >= MAX_PRODUCT_IMAGES) {
            throw new BadRequestException("A product can have maximum 20 images");
        }

        MediaUploadResponse uploaded = cloudinaryService.uploadImage(file, "products");
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(uploaded.getUrl());
        image.setPublicId(uploaded.getPublicId());
        image.setSortOrder(product.getImages().size());
        image.setPrimaryImage(product.getImages().isEmpty());
        product.getImages().add(image);
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse deleteProductImage(User admin, Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        requireProductAccess(admin, product);

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (!image.getProduct().getId().equals(product.getId())) {
            throw new ResourceNotFoundException("Image does not belong to this product");
        }

        if (product.getImages().size() <= MIN_PRODUCT_IMAGES) {
            throw new BadRequestException("A product must have at least 1 image");
        }

        cloudinaryService.deleteAsset(image.getPublicId());
        product.getImages().remove(image);
        productImageRepository.delete(image);

        if (!product.getImages().isEmpty() && product.getImages().stream().noneMatch(ProductImage::isPrimaryImage)) {
            product.getImages().stream().min(Comparator.comparing(ProductImage::getSortOrder)).ifPresent(img -> img.setPrimaryImage(true));
        }

        return toProductResponse(productRepository.save(product));
    }

    public ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .brandLogoUrl(product.getBrand() != null ? product.getBrand().getLogoUrl() : null)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categorySlug(product.getCategory() != null ? product.getCategory().getSlug() : null)
                .modelNumber(product.getModelNumber())
                .processor(product.getProcessor())
                .processorGeneration(product.getProcessorGeneration())
                .ramGb(product.getRamGb())
                .storageGb(product.getStorageGb())
                .storageType(product.getStorageType())
                .displaySize(product.getDisplaySize())
                .displayType(product.getDisplayType())
                .os(product.getOs())
                .graphicsCard(product.getGraphicsCard())
                .battery(product.getBattery())
                .weight(product.getWeight())
                .warrantyMonths(product.getWarrantyMonths())
                .warrantySummary(product.getWarrantySummary())
                .returnDays(product.getReturnDays())
                .sku(product.getSku())
                .serialNumber(product.getSerialNumber())
                .productCondition(product.getProductCondition())
                .productStatus(product.getEffectiveProductStatus())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discountPercent(product.getDiscountPercent())
                .stockQuantity(product.getStockQuantity())
                .available(product.isAvailable())
                .featured(product.isFeatured())
                .bestSeller(product.isBestSellerEnabled())
                .todayDeal(product.isTodayDealEnabled())
                .dealStartDate(product.getDealStartDate())
                .dealEndDate(product.getDealEndDate())
                .displayOrder(product.getResolvedDisplayOrder())
                .videoUrl(product.getVideoUrl())
                .seoTitle(product.getSeoTitle())
                .seoDescription(product.getSeoDescription())
                .seoKeywords(product.getSeoKeywords())
                .lowStockThreshold(product.getResolvedLowStockThreshold())
                .description(product.getDescription())
                .customAttributes(product.getCustomAttributes())
                .stores(product.getStores().stream().map(this::toStoreSummaryResponse).toList())
                .images(product.getImages().stream()
                        .sorted(Comparator.comparing(ProductImage::getSortOrder).thenComparing(ProductImage::getId))
                        .map(image -> ProductImageResponse.builder()
                                .id(image.getId())
                                .imageUrl(image.getImageUrl())
                                .publicId(image.getPublicId())
                                .primaryImage(image.isPrimaryImage())
                                .sortOrder(image.getSortOrder())
                                .build())
                        .toList())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public StoreSummaryResponse toStoreSummaryResponse(Store store) {
        return StoreSummaryResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .address(store.getAddress())
                .landmark(store.getLandmark())
                .postalCode(store.getPostalCode())
                .city(store.getCity())
                .state(store.getState())
                .phone(store.getPhone())
                .whatsapp(store.getWhatsapp())
                .timings(store.getTimings())
                .mapLink(store.getMapLink())
                .imageUrl(store.getImageUrl())
                .videoUrl(store.getVideoUrl())
                .googleRating(store.getGoogleRating())
                .googleReviewCount(store.getGoogleReviewCount())
                .active(store.isActive())
                .build();
    }

    private void applyRequest(Product product, ProductRequest request) {
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        Set<Store> stores = new LinkedHashSet<>(storeRepository.findAllById(request.getStoreIds()));

        if (stores.size() != request.getStoreIds().size()) {
            throw new ResourceNotFoundException("One or more stores were not found");
        }

        product.setTitle(request.getTitle());
        product.setBrand(brand);
        product.setCategory(category);
        product.setStores(stores);
        product.setModelNumber(request.getModelNumber());
        product.setProcessor(request.getProcessor());
        product.setProcessorGeneration(request.getProcessorGeneration());
        product.setRamGb(request.getRamGb());
        product.setStorageGb(request.getStorageGb());
        product.setStorageType(request.getStorageType());
        product.setDisplaySize(request.getDisplaySize());
        product.setDisplayType(request.getDisplayType());
        product.setOs(request.getOs());
        product.setGraphicsCard(request.getGraphicsCard());
        product.setBattery(request.getBattery());
        product.setWeight(request.getWeight());
        product.setWarrantyMonths(request.getWarrantyMonths());
        product.setWarrantySummary(request.getWarrantySummary());
        product.setReturnDays(request.getReturnDays());
        product.setSku(request.getSku());
        product.setSerialNumber(request.getSerialNumber());
        product.setProductCondition(request.getProductCondition());
        product.setProductStatus(resolveProductStatus(product, request.getProductStatus()));
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setDiscountPercent(resolveDiscountPercent(request.getPrice(), request.getOriginalPrice(), request.getDiscountPercent()));
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 1);
        product.setAvailable(request.getAvailable() == null || request.getAvailable());
        product.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        product.setBestSeller(resolveBooleanValue(product.getBestSeller(), request.getBestSeller()));
        product.setTodayDeal(resolveBooleanValue(product.getTodayDeal(), request.getTodayDeal()));
        LocalDateTime resolvedDealStartDate = resolveDateValue(product.getDealStartDate(), request.getDealStartDate(), request.getTodayDeal());
        LocalDateTime resolvedDealEndDate = resolveDateValue(product.getDealEndDate(), request.getDealEndDate(), request.getTodayDeal());
        validateDealWindow(resolvedDealStartDate, resolvedDealEndDate);
        Integer resolvedDisplayOrder = request.getDisplayOrder() != null ? request.getDisplayOrder() : product.getDisplayOrder();
        Integer resolvedLowStockThreshold = request.getLowStockThreshold() != null ? request.getLowStockThreshold() : product.getLowStockThreshold();
        validateDisplayOrder(resolvedDisplayOrder);
        validateLowStockThreshold(resolvedLowStockThreshold);
        product.setDealStartDate(resolvedDealStartDate);
        product.setDealEndDate(resolvedDealEndDate);
        product.setDisplayOrder(resolvedDisplayOrder == null ? 0 : resolvedDisplayOrder);
        product.setVideoUrl(resolveStringValue(product.getVideoUrl(), request.getVideoUrl()));
        product.setSeoTitle(resolveStringValue(product.getSeoTitle(), request.getSeoTitle()));
        product.setSeoDescription(resolveStringValue(product.getSeoDescription(), request.getSeoDescription()));
        product.setSeoKeywords(resolveStringValue(product.getSeoKeywords(), request.getSeoKeywords()));
        product.setLowStockThreshold(resolvedLowStockThreshold == null ? 5 : resolvedLowStockThreshold);
        product.setDescription(request.getDescription());
        product.setCustomAttributes(normalizeCustomAttributes(request.getCustomAttributes()));
    }

    private void attachImages(Product product, List<ProductImageRequest> requestedImages) {
        if (requestedImages == null || requestedImages.isEmpty()) {
            return;
        }

        for (int index = 0; index < requestedImages.size(); index++) {
            ProductImageRequest requestedImage = requestedImages.get(index);
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(requestedImage.getImageUrl());
            image.setPublicId(requestedImage.getPublicId());
            image.setSortOrder(index);
            image.setPrimaryImage(index == 0);
            product.getImages().add(image);
        }
    }

    private void validateImageCount(int imageCount) {
        if (imageCount < MIN_PRODUCT_IMAGES) {
            throw new BadRequestException("A product must have at least 1 image");
        }

        if (imageCount > MAX_PRODUCT_IMAGES) {
            throw new BadRequestException("A product can have maximum 20 images");
        }
    }

    private Map<String, Object> normalizeCustomAttributes(Map<String, Object> customAttributes) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (customAttributes == null || customAttributes.isEmpty()) {
            return normalized;
        }

        for (Map.Entry<String, Object> entry : customAttributes.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            if (key.isEmpty()) {
                continue;
            }

            Object value = normalizeCustomAttributeValue(entry.getValue());
            if (value != null) {
                normalized.put(key, value);
            }
        }

        return normalized;
    }

    private Object normalizeCustomAttributeValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String stringValue) {
            String trimmed = stringValue.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        if (value instanceof List<?> listValue) {
            List<Object> normalizedItems = listValue.stream()
                    .map(this::normalizeCustomAttributeValue)
                    .filter(item -> item != null)
                    .toList();
            return normalizedItems.isEmpty() ? null : normalizedItems;
        }

        return value;
    }

    public boolean isPubliclyVisible(Product product) {
        return product != null
                && product.isAvailable()
                && (product.getProductStatus() == null || product.getEffectiveProductStatus() == ProductStatus.ACTIVE);
    }

    private Specification<Product> publicVisibility() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("available"), true),
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("productStatus")),
                        criteriaBuilder.equal(root.get("productStatus"), ProductStatus.ACTIVE)
                )
        );
    }

    private Specification<Product> matchesQuery(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return null;
        }
        String value = "%" + queryText.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Object, Object> brand = root.join("brand", JoinType.LEFT);
            Join<Object, Object> category = root.join("category", JoinType.LEFT);
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("modelNumber")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("processor")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(brand.get("name")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(category.get("name")), value)
            );
        };
    }

    private Specification<Product> matchesAdminQuery(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return null;
        }
        String value = "%" + queryText.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Object, Object> brand = root.join("brand", JoinType.LEFT);
            Join<Object, Object> category = root.join("category", JoinType.LEFT);
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("modelNumber")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("processor")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("serialNumber")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(brand.get("name")), value),
                    criteriaBuilder.like(criteriaBuilder.lower(category.get("name")), value)
            );
        };
    }

    private Specification<Product> byBrands(List<Long> brandIds) {
        return brandIds == null || brandIds.isEmpty() ? null : (root, query, criteriaBuilder) -> root.get("brand").get("id").in(brandIds);
    }

    private Specification<Product> byCategories(List<Long> categoryIds) {
        return categoryIds == null || categoryIds.isEmpty() ? null : (root, query, criteriaBuilder) -> root.get("category").get("id").in(categoryIds);
    }

    private Specification<Product> byStore(Long storeId) {
        return storeId == null ? null : (root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Object, Object> stores = root.join("stores");
            return criteriaBuilder.equal(stores.get("id"), storeId);
        };
    }

    private Specification<Product> accessibleStores(List<Long> accessibleStoreIds) {
        if (accessibleStoreIds == null || accessibleStoreIds.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Object, Object> stores = root.join("stores", JoinType.LEFT);
            return stores.get("id").in(accessibleStoreIds);
        };
    }

    private Specification<Product> byStores(List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Object, Object> stores = root.join("stores", JoinType.LEFT);
            return stores.get("id").in(storeIds);
        };
    }

    private Specification<Product> byAvailability(Boolean available) {
        return available == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("available"), available);
    }

    private Specification<Product> byFeatured(Boolean featured) {
        return featured == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("featured"), featured);
    }

    private Specification<Product> byBestSeller(Boolean bestSeller) {
        return bestSeller == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("bestSeller"), bestSeller);
    }

    private Specification<Product> byTodayDeal(Boolean todayDeal) {
        return todayDeal == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("todayDeal"), todayDeal);
    }

    private Specification<Product> byStockStates(List<String> stockStates) {
        if (stockStates == null || stockStates.isEmpty()) {
            return null;
        }

        List<String> normalizedStates = stockStates.stream()
                .filter(Objects::nonNull)
                .map(state -> state.trim().toUpperCase())
                .filter(state -> !state.isBlank())
                .distinct()
                .toList();

        if (normalizedStates.isEmpty()) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {
            jakarta.persistence.criteria.Expression<Integer> stockQuantity = root.get("stockQuantity");
            var threshold = criteriaBuilder.<Integer>coalesce();
            threshold.value(root.get("lowStockThreshold"));
            threshold.value(5);

            List<Predicate> predicates = normalizedStates.stream()
                    .map(state -> switch (state) {
                        case "IN_STOCK" -> criteriaBuilder.greaterThan(stockQuantity, threshold);
                        case "LOW_STOCK" -> criteriaBuilder.and(
                                criteriaBuilder.greaterThan(stockQuantity, 0),
                                criteriaBuilder.lessThanOrEqualTo(stockQuantity, threshold)
                        );
                        case "OUT_OF_STOCK" -> criteriaBuilder.lessThanOrEqualTo(stockQuantity, 0);
                        default -> null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (predicates.isEmpty()) {
                return null;
            }

            return criteriaBuilder.or(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<Product> byRamOptions(List<Integer> ramOptions) {
        return ramOptions == null || ramOptions.isEmpty() ? null : (root, query, criteriaBuilder) -> root.get("ramGb").in(ramOptions);
    }

    private Specification<Product> byStorageOptions(List<Integer> storageOptions) {
        return storageOptions == null || storageOptions.isEmpty() ? null : (root, query, criteriaBuilder) -> root.get("storageGb").in(storageOptions);
    }

    private Specification<Product> byProcessors(List<String> processorOptions) {
        if (processorOptions == null || processorOptions.isEmpty()) {
            return null;
        }

        List<String> normalizedValues = processorOptions.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> "%" + value.trim().toLowerCase() + "%")
                .toList();

        if (normalizedValues.isEmpty()) {
            return null;
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                normalizedValues.stream()
                        .map(value -> criteriaBuilder.like(criteriaBuilder.lower(root.get("processor")), value))
                        .toArray(jakarta.persistence.criteria.Predicate[]::new)
        );
    }

    private Specification<Product> byConditions(List<ProductCondition> conditions) {
        return conditions == null || conditions.isEmpty() ? null : (root, query, criteriaBuilder) -> root.get("productCondition").in(conditions);
    }

    private Specification<Product> byStockAvailability(Boolean inStock) {
        if (!Boolean.TRUE.equals(inStock)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get("stockQuantity"), 0);
    }

    private Specification<Product> minPrice(BigDecimal minPrice) {
        return minPrice == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    private Specification<Product> maxPrice(BigDecimal maxPrice) {
        return maxPrice == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    private List<Product> listPublicProductsSorted(Sort sort) {
        return productRepository.findAll(sort).stream()
                .filter(this::isPubliclyVisible)
                .toList();
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 24);
    }

    private void validateDealWindow(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("Deal end date must be after deal start date");
        }
    }

    private void validateDisplayOrder(Integer displayOrder) {
        if (displayOrder != null && displayOrder < 0) {
            throw new BadRequestException("Display order cannot be negative");
        }
    }

    private void validateLowStockThreshold(Integer lowStockThreshold) {
        if (lowStockThreshold != null && lowStockThreshold < 0) {
            throw new BadRequestException("Low stock threshold cannot be negative");
        }
    }

    private ProductStatus resolveProductStatus(Product product, ProductStatus requestedStatus) {
        if (requestedStatus != null) {
            return requestedStatus;
        }
        if (product.getProductStatus() != null) {
            return product.getProductStatus();
        }
        return ProductStatus.ACTIVE;
    }

    private Boolean resolveBooleanValue(Boolean existingValue, Boolean requestedValue) {
        return requestedValue != null ? requestedValue : existingValue;
    }

    private LocalDateTime resolveDateValue(LocalDateTime existingValue, LocalDateTime requestedValue, Boolean toggleValue) {
        if (requestedValue != null) {
            return requestedValue;
        }
        if (toggleValue != null && !toggleValue) {
            return null;
        }
        return existingValue;
    }

    private String resolveStringValue(String existingValue, String requestedValue) {
        if (requestedValue == null) {
            return existingValue;
        }
        return normalizeNullable(requestedValue);
    }

    private Integer resolveDiscountPercent(BigDecimal price, BigDecimal originalPrice, Integer requestedDiscountPercent) {
        if (requestedDiscountPercent != null) {
            if (requestedDiscountPercent < 0 || requestedDiscountPercent > 100) {
                throw new BadRequestException("Discount percentage must be between 0 and 100");
            }
            return requestedDiscountPercent;
        }

        if (price == null || originalPrice == null || originalPrice.signum() <= 0 || originalPrice.compareTo(price) <= 0) {
            return null;
        }

        BigDecimal discount = originalPrice.subtract(price)
                .multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, RoundingMode.HALF_UP);
        return discount.intValue();
    }

    private boolean isDealActive(Product product, LocalDateTime now) {
        if (!product.isTodayDealEnabled()) {
            return false;
        }

        if (product.getDealStartDate() != null && now.isBefore(product.getDealStartDate())) {
            return false;
        }
        if (product.getDealEndDate() != null && now.isAfter(product.getDealEndDate())) {
            return false;
        }
        return true;
    }

    private boolean isDealActive(ProductResponse product, LocalDateTime now) {
        if (product == null || !product.getIsTodayDeal()) {
            return false;
        }

        if (product.getDealStartDate() != null && now.isBefore(product.getDealStartDate())) {
            return false;
        }
        if (product.getDealEndDate() != null && now.isAfter(product.getDealEndDate())) {
            return false;
        }
        return true;
    }

    private int discountValue(Product product) {
        return product.getDiscountPercent() == null ? 0 : product.getDiscountPercent();
    }

    private int discountValue(ProductResponse product) {
        return product.getDiscountPercent() == null ? 0 : product.getDiscountPercent();
    }

    private void applyPriceAdjustment(Product product, BigDecimal adjustmentPercent) {
        if (adjustmentPercent == null) {
            throw new BadRequestException("Price adjustment percentage is required");
        }

        BigDecimal multiplier = BigDecimal.ONE.add(adjustmentPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal adjustedPrice = product.getPrice()
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);

        if (adjustedPrice.signum() <= 0) {
            throw new BadRequestException("Adjusted price must stay above zero");
        }

        product.setPrice(adjustedPrice);
        product.setDiscountPercent(resolveDiscountPercent(adjustedPrice, product.getOriginalPrice(), null));
    }

    private void applyTodayDealState(Product product, boolean enabled) {
        product.setTodayDeal(enabled);
        if (!enabled) {
            product.setDealStartDate(null);
            product.setDealEndDate(null);
            return;
        }

        if (product.getDealStartDate() == null) {
            product.setDealStartDate(LocalDateTime.now());
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateRequestedStoreAccess(User admin, List<Long> storeIds) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        if (accessibleStoreIds.isEmpty()) {
            return;
        }
        boolean invalidAssignment = storeIds.stream().anyMatch(storeId -> !accessibleStoreIds.contains(storeId));
        if (invalidAssignment) {
            throw new AccessDeniedException("One or more selected stores are outside your assigned scope");
        }
    }

    private void requireProductAccess(User admin, Product product) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        if (!canAccessProduct(accessibleStoreIds, product)) {
            throw new AccessDeniedException("You do not have access to this product");
        }
    }

    private boolean canAccessProduct(List<Long> accessibleStoreIds, Product product) {
        if (accessibleStoreIds == null || accessibleStoreIds.isEmpty()) {
            return true;
        }
        return product.getStores().stream().map(Store::getId).anyMatch(accessibleStoreIds::contains);
    }
}
