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
@Table(name = "site_settings")
public class SiteSettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    private String supportEmail;

    private String supportPhone;

    @Column(length = 1000)
    private String shippingNote;

    @Column(length = 1000)
    private String returnPolicy;

    private String defaultCity;

    private String defaultState;

    @Column(length = 500)
    private String mapLink;
}
