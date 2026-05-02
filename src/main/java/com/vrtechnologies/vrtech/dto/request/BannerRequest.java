package com.vrtechnologies.vrtech.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.vrtechnologies.vrtech.entity.enums.BannerMediaType;
import com.vrtechnologies.vrtech.entity.enums.BannerPlacement;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BannerRequest {

    private String title;
    private String subtitle;

    private String imageUrl;

    @JsonAlias("desktopImage")
    private String desktopImageUrl;

    @JsonAlias("mobileImage")
    private String mobileImageUrl;

    private String videoUrl;

    private BannerMediaType mediaType;

    private String ctaText;

    @JsonAlias("ctaLink")
    private String linkUrl;

    private BannerPlacement placement;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Boolean active;
    private Integer sortOrder;
}
