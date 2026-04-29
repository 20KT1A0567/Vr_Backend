package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.BannerRequest;
import com.vrtechnologies.vrtech.dto.request.BrandRequest;
import com.vrtechnologies.vrtech.dto.request.CategoryRequest;
import com.vrtechnologies.vrtech.dto.request.StoreRequest;
import com.vrtechnologies.vrtech.entity.Banner;
import com.vrtechnologies.vrtech.entity.Brand;
import com.vrtechnologies.vrtech.entity.Category;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.BannerRepository;
import com.vrtechnologies.vrtech.repository.BrandRepository;
import com.vrtechnologies.vrtech.repository.CategoryRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final BannerRepository bannerRepository;

    public CatalogService(
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            StoreRepository storeRepository,
            BannerRepository bannerRepository
    ) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.bannerRepository = bannerRepository;
    }

    public List<Brand> getBrands() {
        return brandRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<Category> getCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<Store> getStores() {
        return getStores(false);
    }

    public List<Store> getStores(boolean includeInactive) {
        if (includeInactive) {
            return storeRepository.findAllByOrderByCityAscNameAsc();
        }
        return storeRepository.findByActiveTrueOrderByCityAscNameAsc();
    }

    public List<Banner> getBanners() {
        return getBanners(false);
    }

    public List<Banner> getBanners(boolean includeInactive) {
        if (includeInactive) {
            return bannerRepository.findAllByOrderBySortOrderAscIdDesc();
        }
        return bannerRepository.findByActiveTrueOrderBySortOrderAscIdDesc();
    }

    public Banner saveBanner(BannerRequest request, Long id) {
        Banner banner = id == null ? new Banner() : bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setVideoUrl(request.getVideoUrl());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setActive(request.getActive() == null || request.getActive());
        banner.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        return bannerRepository.save(banner);
    }

    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }

    public Brand createBrand(BrandRequest request) {
        return saveBrand(request, null);
    }

    public Brand saveBrand(BrandRequest request, Long id) {
        Brand brand = id == null ? new Brand() : brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        brand.setName(request.getName());
        brand.setLogoUrl(request.getLogoUrl());
        try {
            return brandRepository.save(brand);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Brand name already exists");
        }
    }

    public void deleteBrand(Long id) {
        try {
            brandRepository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Brand cannot be deleted while products are linked to it");
        }
    }

    public Category createCategory(CategoryRequest request) {
        return saveCategory(request, null);
    }

    public Category saveCategory(CategoryRequest request, Long id) {
        Category category = id == null ? new Category() : categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setIconUrl(request.getIconUrl());
        try {
            return categoryRepository.save(category);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Category name or slug already exists");
        }
    }

    public void deleteCategory(Long id) {
        try {
            categoryRepository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Category cannot be deleted while products are linked to it");
        }
    }

    public Store saveStore(StoreRequest request, Long id) {
        Store store = id == null ? new Store() : storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        store.setName(request.getName());
        store.setAddress(request.getAddress());
        store.setCity(request.getCity());
        store.setState(request.getState() == null || request.getState().isBlank() ? "Andhra Pradesh" : request.getState());
        store.setPhone(request.getPhone());
        store.setWhatsapp(request.getWhatsapp());
        store.setTimings(request.getTimings());
        store.setMapLink(request.getMapLink());
        store.setImageUrl(request.getImageUrl());
        store.setVideoUrl(request.getVideoUrl());
        store.setActive(request.getActive() == null || request.getActive());
        return storeRepository.save(store);
    }
}
