package com.vrtechnologies.vrtech.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CmsPageResponse {
    private Long id;
    private String slug;
    private String title;
    private String metaTitle;
    private String metaDescription;
    private String eyebrow;
    private String heroTitle;
    private String heroDescription;
    private String body;
    private boolean active;
    private List<CmsPageSectionResponse> sections;
    private List<CmsPageFaqItemResponse> faqItems;
    private LocalDateTime updatedAt;
}
