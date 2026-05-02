package com.vrtechnologies.vrtech.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vrtechnologies.vrtech.entity.enums.BannerMediaType;
import com.vrtechnologies.vrtech.entity.enums.BannerPlacement;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BannerResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String desktopImageUrl;
    private String mobileImageUrl;
    private String videoUrl;
    private BannerMediaType mediaType;
    private String ctaText;
    private String linkUrl;
    private BannerPlacement placement;
    private boolean active;
    private boolean activeNow;
    private Integer sortOrder;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @JsonProperty("desktopImage")
    public String getDesktopImage() {
        return desktopImageUrl;
    }

    @JsonProperty("mobileImage")
    public String getMobileImage() {
        return mobileImageUrl;
    }

    @JsonProperty("ctaLink")
    public String getCtaLink() {
        return linkUrl;
    }
}
