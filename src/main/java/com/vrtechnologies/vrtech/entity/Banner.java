package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.entity.enums.BannerMediaType;
import com.vrtechnologies.vrtech.entity.enums.BannerPlacement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "banners")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String desktopImageUrl;

    @Column(length = 500)
    private String mobileImageUrl;

    @Column(length = 500)
    private String videoUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BannerMediaType mediaType;

    @Column(length = 120)
    private String ctaText;

    @Column(length = 500)
    private String linkUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BannerPlacement placement;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    public BannerMediaType getEffectiveMediaType() {
        if (mediaType != null) {
            return mediaType;
        }
        return videoUrl != null && !videoUrl.isBlank() ? BannerMediaType.VIDEO : BannerMediaType.IMAGE;
    }

    public BannerPlacement getEffectivePlacement() {
        return placement == null ? BannerPlacement.HOME_HERO : placement;
    }

    public String getResolvedDesktopImageUrl() {
        if (desktopImageUrl != null && !desktopImageUrl.isBlank()) {
            return desktopImageUrl;
        }
        return imageUrl;
    }

    public String getResolvedMobileImageUrl() {
        if (mobileImageUrl != null && !mobileImageUrl.isBlank()) {
            return mobileImageUrl;
        }
        return getResolvedDesktopImageUrl();
    }
}
