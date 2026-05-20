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
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String iconUrl;

    @Column(name = "compare_fields", length = 1000)
    private String compareFields;

    private String seoTitle;

    @Column(columnDefinition = "TEXT")
    private String seoDescription;

    @Column(columnDefinition = "TEXT")
    private String seoKeywords;

    private String ogImageUrl;

    private String canonicalUrl;
}
