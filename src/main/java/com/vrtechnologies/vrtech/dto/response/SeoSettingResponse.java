package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeoSettingResponse {
    private Long id;
    private String targetType;
    private Long targetId;
    private String targetSlug;
    private String pageTitle;
    private String metaDescription;
    private String metaKeywords;
    private String ogImageUrl;
    private String canonicalUrl;
    private boolean noIndex;
    private boolean sitemapEnabled;
}
