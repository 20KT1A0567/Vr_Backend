package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.SeoSettingRequest;
import com.vrtechnologies.vrtech.dto.response.SeoSettingResponse;
import com.vrtechnologies.vrtech.entity.SeoSetting;
import com.vrtechnologies.vrtech.repository.SeoSettingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class SeoSettingService {

    private final SeoSettingRepository seoSettingRepository;

    public SeoSettingService(SeoSettingRepository seoSettingRepository) {
        this.seoSettingRepository = seoSettingRepository;
    }

    public List<SeoSettingResponse> list() {
        return seoSettingRepository.findAllByOrderByTargetTypeAscTargetSlugAscTargetIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<SeoSettingResponse> find(String targetType, Long targetId, String targetSlug) {
        return findEntity(targetType, targetId, targetSlug).map(this::toResponse);
    }

    public Optional<SeoSetting> findEntity(String targetType, Long targetId, String targetSlug) {
        String normalizedType = normalizeType(targetType);
        if (targetId != null) {
            return seoSettingRepository.findFirstByTargetTypeAndTargetId(normalizedType, targetId);
        }
        String normalizedSlug = normalizeString(targetSlug);
        if (normalizedSlug != null) {
            return seoSettingRepository.findFirstByTargetTypeAndTargetSlugIgnoreCase(normalizedType, normalizedSlug);
        }
        return seoSettingRepository.findFirstByTargetTypeAndTargetIdIsNullAndTargetSlugIsNull(normalizedType);
    }

    public SeoSettingResponse save(SeoSettingRequest request) {
        String targetType = normalizeType(request.getTargetType());
        String targetSlug = normalizeString(request.getTargetSlug());
        Long targetId = request.getTargetId();

        SeoSetting setting = findEntity(targetType, targetId, targetSlug).orElseGet(SeoSetting::new);
        setting.setTargetType(targetType);
        setting.setTargetId(targetId);
        setting.setTargetSlug(targetId == null ? targetSlug : null);
        setting.setPageTitle(normalizeString(request.getPageTitle()));
        setting.setMetaDescription(normalizeString(request.getMetaDescription()));
        setting.setMetaKeywords(normalizeString(request.getMetaKeywords()));
        setting.setOgImageUrl(normalizeString(request.getOgImageUrl()));
        setting.setCanonicalUrl(normalizeString(request.getCanonicalUrl()));
        setting.setNoIndex(Boolean.TRUE.equals(request.getNoIndex()));
        setting.setSitemapEnabled(request.getSitemapEnabled() == null || request.getSitemapEnabled());
        return toResponse(seoSettingRepository.save(setting));
    }

    public SeoSettingResponse toResponse(SeoSetting setting) {
        return SeoSettingResponse.builder()
                .id(setting.getId())
                .targetType(setting.getTargetType())
                .targetId(setting.getTargetId())
                .targetSlug(setting.getTargetSlug())
                .pageTitle(setting.getPageTitle())
                .metaDescription(setting.getMetaDescription())
                .metaKeywords(setting.getMetaKeywords())
                .ogImageUrl(setting.getOgImageUrl())
                .canonicalUrl(setting.getCanonicalUrl())
                .noIndex(setting.isNoIndex())
                .sitemapEnabled(setting.isSitemapEnabled())
                .build();
    }

    private String normalizeType(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
