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
@Table(name = "cms_pages")
public class CmsPage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 200)
    private String metaTitle;

    @Column(length = 4000)
    private String metaDescription;

    @Column(length = 80)
    private String eyebrow;

    @Column(length = 200)
    private String heroTitle;

    @Column(length = 4000)
    private String heroDescription;

    @Column(length = 16000)
    private String body;

    @Column(name = "sections_json", columnDefinition = "LONGTEXT")
    private String sectionsJson;

    @Column(name = "faq_items_json", columnDefinition = "LONGTEXT")
    private String faqItemsJson;

    @Column(nullable = false)
    private boolean active = true;
}
