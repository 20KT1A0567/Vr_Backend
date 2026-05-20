package com.vrtechnologies.vrtech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "seo_settings")
public class SeoSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String targetType;

    private Long targetId;

    @Column(length = 120)
    private String targetSlug;

    @Column(length = 220)
    private String pageTitle;

    @Column(length = 500)
    private String metaDescription;

    @Column(length = 1000)
    private String metaKeywords;

    @Column(length = 500)
    private String ogImageUrl;

    @Column(length = 500)
    private String canonicalUrl;

    @Column(nullable = false)
    private boolean noIndex;

    @Column(nullable = false)
    private boolean sitemapEnabled = true;
}
