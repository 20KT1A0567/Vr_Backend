package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.NavigationConfigRequest;
import com.vrtechnologies.vrtech.dto.request.NavigationItemRequest;
import com.vrtechnologies.vrtech.dto.response.NavigationConfigResponse;
import com.vrtechnologies.vrtech.dto.response.NavigationItemResponse;
import com.vrtechnologies.vrtech.entity.NavigationItem;
import com.vrtechnologies.vrtech.entity.enums.NavigationMenuLocation;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.repository.NavigationItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class NavigationService {

    private final NavigationItemRepository navigationItemRepository;

    public NavigationService(NavigationItemRepository navigationItemRepository) {
        this.navigationItemRepository = navigationItemRepository;
    }

    @Transactional
    public NavigationConfigResponse getAdminConfig() {
        ensureDefaults();
        return toConfigResponse(navigationItemRepository.findAllByOrderByMenuLocationAscSortOrderAscIdAsc(), false);
    }

    @Transactional
    public NavigationConfigResponse getPublicConfig() {
        ensureDefaults();
        return toConfigResponse(navigationItemRepository.findByVisibleTrueOrderByMenuLocationAscSortOrderAscIdAsc(), true);
    }

    @Transactional
    public NavigationConfigResponse saveConfig(NavigationConfigRequest request) {
        ensureDefaults();
        List<NavigationItemRequest> items = request.getItems() == null ? List.of() : request.getItems();
        if (items.isEmpty()) {
            throw new BadRequestException("Navigation items are required");
        }

        navigationItemRepository.deleteAllInBatch();

        List<NavigationItem> entities = new ArrayList<>();
        for (NavigationItemRequest item : items) {
            NavigationItem entity = new NavigationItem();
            entity.setMenuLocation(item.getMenuLocation());
            entity.setLabel(item.getLabel().trim());
            entity.setUrl(item.getUrl().trim());
            entity.setVisible(item.getVisible() == null || item.getVisible());
            entity.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
            entities.add(entity);
        }

        navigationItemRepository.saveAll(entities);
        return getAdminConfig();
    }

    @Transactional
    public void ensureDefaults() {
        if (navigationItemRepository.count() > 0) {
            return;
        }

        List<NavigationItem> seeds = new ArrayList<>();
        seeds.add(seed(NavigationMenuLocation.HEADER, "Home", "/", 1, true));
        seeds.add(seed(NavigationMenuLocation.HEADER, "Products", "/products", 2, true));
        seeds.add(seed(NavigationMenuLocation.HEADER, "About Us", "/about", 3, true));
        seeds.add(seed(NavigationMenuLocation.HEADER, "Contact", "/contact", 4, true));
        seeds.add(seed(NavigationMenuLocation.HEADER, "Privacy Policy", "/privacy", 5, false));

        seeds.add(seed(NavigationMenuLocation.FOOTER, "Home", "/", 1, true));
        seeds.add(seed(NavigationMenuLocation.FOOTER, "Products", "/products", 2, true));
        seeds.add(seed(NavigationMenuLocation.FOOTER, "About Us", "/about", 3, true));
        seeds.add(seed(NavigationMenuLocation.FOOTER, "Contact", "/contact", 4, true));
        seeds.add(seed(NavigationMenuLocation.FOOTER, "Privacy Policy", "/privacy", 5, true));
        seeds.add(seed(NavigationMenuLocation.FOOTER, "Terms & Conditions", "/terms", 6, true));
        seeds.add(seed(NavigationMenuLocation.FOOTER, "Shipping Policy", "/shipping", 7, true));
        seeds.add(seed(NavigationMenuLocation.FOOTER, "Warranty Policy", "/warranty", 8, true));
        seeds.add(seed(NavigationMenuLocation.FOOTER, "Returns Policy", "/returns", 9, true));

        seeds.add(seed(NavigationMenuLocation.MOBILE, "Home", "/", 1, true));
        seeds.add(seed(NavigationMenuLocation.MOBILE, "Products", "/products", 2, true));
        seeds.add(seed(NavigationMenuLocation.MOBILE, "About Us", "/about", 3, true));
        seeds.add(seed(NavigationMenuLocation.MOBILE, "Contact", "/contact", 4, true));
        seeds.add(seed(NavigationMenuLocation.MOBILE, "Privacy Policy", "/privacy", 5, false));

        navigationItemRepository.saveAll(seeds);
    }

    private NavigationItem seed(NavigationMenuLocation location, String label, String url, int sortOrder, boolean visible) {
        NavigationItem item = new NavigationItem();
        item.setMenuLocation(location);
        item.setLabel(label);
        item.setUrl(url);
        item.setSortOrder(sortOrder);
        item.setVisible(visible);
        return item;
    }

    private NavigationConfigResponse toConfigResponse(List<NavigationItem> items, boolean visibleOnly) {
        Map<NavigationMenuLocation, List<NavigationItemResponse>> grouped = new EnumMap<>(NavigationMenuLocation.class);
        for (NavigationMenuLocation location : NavigationMenuLocation.values()) {
            grouped.put(location, new ArrayList<>());
        }

        items.stream()
                .filter(item -> !visibleOnly || item.isVisible())
                .sorted(Comparator.comparing(NavigationItem::getMenuLocation)
                        .thenComparing(item -> item.getSortOrder() == null ? 0 : item.getSortOrder())
                        .thenComparing(NavigationItem::getId))
                .map(this::toResponse)
                .forEach(item -> grouped.get(item.getMenuLocation()).add(item));

        return NavigationConfigResponse.builder()
                .headerMenu(grouped.getOrDefault(NavigationMenuLocation.HEADER, List.of()))
                .footerMenu(grouped.getOrDefault(NavigationMenuLocation.FOOTER, List.of()))
                .mobileMenu(grouped.getOrDefault(NavigationMenuLocation.MOBILE, List.of()))
                .build();
    }

    private NavigationItemResponse toResponse(NavigationItem item) {
        return NavigationItemResponse.builder()
                .id(item.getId())
                .menuLocation(item.getMenuLocation())
                .label(item.getLabel())
                .url(item.getUrl())
                .visible(item.isVisible())
                .sortOrder(item.getSortOrder())
                .build();
    }
}
