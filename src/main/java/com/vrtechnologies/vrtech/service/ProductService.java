package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.ProductRequest;
import com.vrtechnologies.vrtech.dto.request.ProductImageRequest;
import com.vrtechnologies.vrtech.dto.response.MediaUploadResponse;
import com.vrtechnologies.vrtech.dto.response.ProductImageResponse;
import com.vrtechnologies.vrtech.dto.response.ProductResponse;
import com.vrtechnologies.vrtech.dto.response.StoreSummaryResponse;
import com.vrtechnologies.vrtech.entity.Brand;
import com.vrtechnologies.vrtech.entity.Category;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.ProductImage;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.enums.ProductCondition;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.BrandRepository;
import com.vrtechnologies.vrtech.repository.CategoryRepository;
import com.vrtechnologies.vrtech.repository.ProductImageRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
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

    public ProductService(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            StoreRepository storeRepository,
            ProductImageRepository productImageRepository,
            CloudinaryService cloudinaryService
    ) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.productImageRepository = productImageRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public List<ProductResponse> getPublicProducts(
            String query,
            List<Long> brandIds,
            List<Long> categoryIds,
            Long storeId,
            List<Integer> ramOptions,
            List<Integer> storageOptions,
            List<String> processorOptions,
            ProductCondition condition,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        Specification<Product> specification = Specification.where(available(true))
                .and(matchesQuery(query))
                .and(byBrands(brandIds))
                .and(byCategories(categoryIds))
                .and(byStore(storeId))
                .and(byRamOptions(ramOptions))
                .and(byStorageOptions(storageOptions))
                .and(byProcessors(processorOptions))
                .and(byCondition(condition))
                .and(minPrice(minPrice))
                .and(maxPrice(maxPrice));

        return productRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    public List<ProductResponse> getAllProductsForAdmin() {
        return productRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    public List<ProductResponse> getFeaturedProducts() {
        return productRepository.findTop8ByFeaturedTrueAndAvailableTrueOrderByUpdatedAtDesc()
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    public ProductResponse getProduct(Long id, boolean adminView) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!adminView && !product.isAvailable()) {
            throw new ResourceNotFoundException("Product not found");
        }

        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        validateImageCount(request.getImages() == null ? 0 : request.getImages().size());
        Product product = new Product();
        applyRequest(product, request);
        attachImages(product, request.getImages());
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        applyRequest(product, request);
        validateImageCount(product.getImages().size());
        return toProductResponse(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        productRepository.delete(product);
    }

    @Transactional
    public ProductResponse uploadProductImage(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

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
    public ProductResponse deleteProductImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

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
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discountPercent(product.getDiscountPercent())
                .stockQuantity(product.getStockQuantity())
                .available(product.isAvailable())
                .featured(product.isFeatured())
                .description(product.getDescription())
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
                .city(store.getCity())
                .state(store.getState())
                .phone(store.getPhone())
                .whatsapp(store.getWhatsapp())
                .timings(store.getTimings())
                .mapLink(store.getMapLink())
                .imageUrl(store.getImageUrl())
                .videoUrl(store.getVideoUrl())
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
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setDiscountPercent(request.getDiscountPercent());
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 1);
        product.setAvailable(request.getAvailable() == null || request.getAvailable());
        product.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        product.setDescription(request.getDescription());
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

    private Specification<Product> available(boolean available) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("available"), available);
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

    private Specification<Product> byCondition(ProductCondition condition) {
        return condition == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("productCondition"), condition);
    }

    private Specification<Product> minPrice(BigDecimal minPrice) {
        return minPrice == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    private Specification<Product> maxPrice(BigDecimal maxPrice) {
        return maxPrice == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
