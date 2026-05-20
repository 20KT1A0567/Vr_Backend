package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.entity.enums.NavigationMenuLocation;
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

@Getter
@Setter
@Entity
@Table(name = "navigation_items")
public class NavigationItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NavigationMenuLocation menuLocation;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false, length = 255)
    private String url;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
