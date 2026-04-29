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
@Table(name = "banners")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String videoUrl;

    @Column(length = 500)
    private String linkUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
