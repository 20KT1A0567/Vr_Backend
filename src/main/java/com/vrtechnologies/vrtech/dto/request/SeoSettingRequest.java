package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeoSettingRequest {

    @NotBlank
    private String targetType;

    private Long targetId;
    private String targetSlug;
    private String pageTitle;
    private String metaDescription;
    private String metaKeywords;
    private String ogImageUrl;
    private String canonicalUrl;
    private Boolean noIndex;
    private Boolean sitemapEnabled;
}
