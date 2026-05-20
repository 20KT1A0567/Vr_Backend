package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String iconUrl;

    private String compareFields;

    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    private String ogImageUrl;
    private String canonicalUrl;
}
