package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CmsPageRequest {

    @NotBlank
    private String title;

    private String metaTitle;
    private String metaDescription;
    private String eyebrow;
    private String heroTitle;
    private String heroDescription;
    private String body;
    private Boolean active;
    private List<CmsPageSectionRequest> sections;
    private List<CmsPageFaqItemRequest> faqItems;
}
