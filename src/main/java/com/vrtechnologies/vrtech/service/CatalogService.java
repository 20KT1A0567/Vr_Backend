package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.BannerRequest;
import com.vrtechnologies.vrtech.dto.request.BrandRequest;
import com.vrtechnologies.vrtech.dto.request.CategoryRequest;
import com.vrtechnologies.vrtech.dto.request.StoreRequest;
import com.vrtechnologies.vrtech.dto.response.BannerResponse;
import com.vrtechnologies.vrtech.entity.Banner;
import com.vrtechnologies.vrtech.entity.Brand;
import com.vrtechnologies.vrtech.entity.Category;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.BannerMediaType;
import com.vrtechnologies.vrtech.entity.enums.BannerPlacement;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.BannerRepository;
import com.vrtechnologies.vrtech.repository.BrandRepository;
import com.vrtechnologies.vrtech.repository.CategoryRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class CatalogService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final BannerRepository bannerRepository;
    private final PermissionService permissionService;

    public CatalogService(
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            StoreRepository storeRepository,
            BannerRepository bannerRepository,
            PermissionService permissionService
    ) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.bannerRepository = bannerRepository;
        this.permissionService = permissionService;
    }

    @Cacheable("brands")
    public List<Brand> getBrands() {
        return brandRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Cacheable("categories")
    public List<Category> getCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Cacheable("stores")
    public List<Store> getStores() {
        return getStores(false);
    }

    public List<Store> getStores(boolean includeInactive) {
        if (includeInactive) {
            return storeRepository.findAllByOrderByCityAscNameAsc();
        }
        return storeRepository.findByActiveTrueOrderByCityAscNameAsc();
    }

    public List<Store> getStores(boolean includeInactive, User admin) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        return getStores(includeInactive).stream()
                .filter(store -> accessibleStoreIds.isEmpty() || accessibleStoreIds.contains(store.getId()))
                .toList();
    }

    @Cacheable("catalog")
    public List<BannerResponse> getBanners() {
        return getBanners(false, null);
    }

    public List<BannerResponse> getBanners(boolean includeInactive) {
        return getBanners(includeInactive, null);
    }

    public List<BannerResponse> getBanners(boolean includeInactive, BannerPlacement placement) {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        return bannerRepository.findAllByOrderBySortOrderAscIdDesc().stream()
                .filter(banner -> placement == null || banner.getEffectivePlacement() == placement)
                .filter(banner -> includeInactive || isBannerActiveNow(banner, now))
                .map(banner -> toBannerResponse(banner, now))
                .toList();
    }

    @CacheEvict(value = "catalog", allEntries = true)
    public BannerResponse saveBanner(BannerRequest request, Long id) {
        Banner banner = id == null ? new Banner() : bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
        String desktopImageUrl = resolveDesktopImageUrl(request, banner);
        String mobileImageUrl = resolveMobileImageUrl(request, banner, desktopImageUrl);
        String videoUrl = resolveStringValue(banner.getVideoUrl(), request.getVideoUrl());
        BannerMediaType mediaType = resolveMediaType(banner, request, videoUrl);
        LocalDateTime resolvedStartAt = resolveDateValue(banner.getStartAt(), request.getStartAt(), request.isStartAtProvided());
        LocalDateTime resolvedEndAt = resolveDateValue(banner.getEndAt(), request.getEndAt(), request.isEndAtProvided());
        validateBannerRequest(desktopImageUrl, mediaType, videoUrl, resolvedStartAt, resolvedEndAt);
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setImageUrl(desktopImageUrl);
        banner.setDesktopImageUrl(desktopImageUrl);
        banner.setMobileImageUrl(mobileImageUrl);
        banner.setVideoUrl(videoUrl);
        banner.setMediaType(mediaType);
        banner.setCtaText(resolveStringValue(banner.getCtaText(), request.getCtaText()));
        banner.setLinkUrl(resolveStringValue(banner.getLinkUrl(), request.getLinkUrl()));
        banner.setPlacement(request.getPlacement() != null ? request.getPlacement() : banner.getEffectivePlacement());
        banner.setStartAt(resolvedStartAt);
        banner.setEndAt(resolvedEndAt);
        banner.setActive(request.getActive() == null || request.getActive());
        banner.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        return toBannerResponse(bannerRepository.save(banner));
    }

    @CacheEvict(value = "catalog", allEntries = true)
    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }

    @CacheEvict(value = "brands", allEntries = true)
    public Brand createBrand(BrandRequest request) {
        return saveBrand(request, null);
    }

    @CacheEvict(value = "brands", allEntries = true)
    public Brand saveBrand(BrandRequest request, Long id) {
        String normalizedName = request.getName() == null ? "" : request.getName().trim();
        if (normalizedName.isBlank()) {
            throw new BadRequestException("Brand name is required");
        }

        brandRepository.findByNameIgnoreCase(normalizedName)
                .filter(existing -> id == null || !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Brand name already exists");
                });

        Brand brand = id == null ? new Brand() : brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        brand.setName(normalizedName);
        brand.setLogoUrl(request.getLogoUrl() == null || request.getLogoUrl().isBlank() ? null : request.getLogoUrl().trim());
        brand.setDescription(request.getDescription() == null || request.getDescription().isBlank() ? null : request.getDescription().trim());
        brand.setSortOrder(request.getSortOrder() == null ? 0 : Math.max(0, request.getSortOrder()));
        brand.setDiscountPercent(request.getDiscountPercent() == null ? null : Math.max(0, request.getDiscountPercent()));
        brand.setActive(request.getActive() == null || request.getActive());
        try {
            return brandRepository.save(brand);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Brand name already exists");
        }
    }

    @CacheEvict(value = "brands", allEntries = true)
    public void deleteBrand(Long id) {
        try {
            brandRepository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Brand cannot be deleted while products are linked to it");
        }
    }

    @CacheEvict(value = "categories", allEntries = true)
    public Category createCategory(CategoryRequest request) {
        return saveCategory(request, null);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public Category saveCategory(CategoryRequest request, Long id) {
        Category category = id == null ? new Category() : categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setIconUrl(request.getIconUrl());
        category.setCompareFields(request.getCompareFields());
        category.setSeoTitle(request.getSeoTitle());
        category.setSeoDescription(request.getSeoDescription());
        category.setSeoKeywords(request.getSeoKeywords());
        category.setOgImageUrl(request.getOgImageUrl());
        category.setCanonicalUrl(request.getCanonicalUrl());
        try {
            return categoryRepository.save(category);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Category name or slug already exists");
        }
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        try {
            categoryRepository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Category cannot be deleted while products are linked to it");
        }
    }

    @CacheEvict(value = "stores", allEntries = true)
    public Store saveStore(User admin, StoreRequest request, Long id) {
        Store store = id == null ? new Store() : storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        if (id != null) {
            permissionService.requireStoreAccess(admin, id);
        }
        store.setName(request.getName());
        store.setAddress(request.getAddress());
        store.setLandmark(request.getLandmark());
        store.setPostalCode(request.getPostalCode());
        store.setCity(request.getCity());
        store.setState(request.getState() == null || request.getState().isBlank() ? "Andhra Pradesh" : request.getState());
        store.setPhone(request.getPhone());
        store.setWhatsapp(request.getWhatsapp());
        store.setTimings(request.getTimings());
        store.setMapLink(request.getMapLink());
        store.setImageUrl(request.getImageUrl());
        store.setVideoUrl(request.getVideoUrl());
        store.setGoogleRating(request.getGoogleRating());
        store.setGoogleReviewCount(request.getGoogleReviewCount());
        store.setActive(request.getActive() == null || request.getActive());
        return storeRepository.save(store);
    }

    @CacheEvict(value = "stores", allEntries = true)
    public void deleteStore(User admin, Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        permissionService.requireStoreAccess(admin, id);
        try {
            storeRepository.delete(store);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Store cannot be deleted while products or orders are linked to it");
        }
    }

    private BannerResponse toBannerResponse(Banner banner) {
        return toBannerResponse(banner, LocalDateTime.now(BUSINESS_ZONE));
    }

    private BannerResponse toBannerResponse(Banner banner, LocalDateTime now) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .imageUrl(banner.getImageUrl())
                .desktopImageUrl(banner.getResolvedDesktopImageUrl())
                .mobileImageUrl(banner.getResolvedMobileImageUrl())
                .videoUrl(banner.getVideoUrl())
                .mediaType(banner.getEffectiveMediaType())
                .ctaText(banner.getCtaText())
                .linkUrl(banner.getLinkUrl())
                .placement(banner.getEffectivePlacement())
                .active(banner.isActive())
                .activeNow(isBannerActiveNow(banner, now))
                .sortOrder(banner.getSortOrder())
                .startAt(banner.getStartAt())
                .endAt(banner.getEndAt())
                .build();
    }

    private boolean isBannerActiveNow(Banner banner, LocalDateTime now) {
        if (!banner.isActive()) {
            return false;
        }
        if (banner.getStartAt() != null && now.isBefore(banner.getStartAt())) {
            return false;
        }
        if (banner.getEndAt() != null && now.isAfter(banner.getEndAt())) {
            return false;
        }
        return true;
    }

    private void validateBannerRequest(String desktopImageUrl, BannerMediaType mediaType, String videoUrl, LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new BadRequestException("Banner end date must be after start date");
        }

        if (mediaType == BannerMediaType.IMAGE && desktopImageUrl == null) {
            throw new BadRequestException("Banner desktop image is required");
        }

        if (mediaType == BannerMediaType.VIDEO && videoUrl == null) {
            throw new BadRequestException("Video banner requires a video URL");
        }
    }

    private BannerMediaType resolveMediaType(Banner banner, BannerRequest request, String videoUrl) {
        if (request.getMediaType() != null) {
            return request.getMediaType();
        }
        if (banner.getMediaType() != null) {
            return banner.getMediaType();
        }
        return videoUrl != null ? BannerMediaType.VIDEO : BannerMediaType.IMAGE;
    }

    private String resolveDesktopImageUrl(BannerRequest request, Banner banner) {
        String desktopImageUrl = normalizeString(request.getDesktopImageUrl());
        if (desktopImageUrl != null) {
            return desktopImageUrl;
        }
        String imageUrl = normalizeString(request.getImageUrl());
        if (imageUrl != null) {
            return imageUrl;
        }
        return banner.getResolvedDesktopImageUrl();
    }

    private String resolveMobileImageUrl(BannerRequest request, Banner banner, String desktopImageUrl) {
        String mobileImageUrl = normalizeString(request.getMobileImageUrl());
        if (mobileImageUrl != null) {
            return mobileImageUrl;
        }
        if (banner.getMobileImageUrl() != null && !banner.getMobileImageUrl().isBlank()) {
            return banner.getMobileImageUrl();
        }
        return desktopImageUrl;
    }

    private String resolveStringValue(String existingValue, String requestedValue) {
        if (requestedValue == null) {
            return existingValue;
        }
        return normalizeString(requestedValue);
    }

    private LocalDateTime resolveDateValue(LocalDateTime existingValue, LocalDateTime requestedValue, boolean provided) {
        return provided ? requestedValue : existingValue;
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
