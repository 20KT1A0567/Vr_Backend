package com.vrtechnologies.vrtech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "pincode_delivery_rules")
public class PincodeDeliveryRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 6)
    private String pincode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode = "IN";

    @Column(name = "state_name", length = 80)
    private String stateName;

    @Column(name = "city_name", length = 120)
    private String cityName;

    @Column(name = "zone_name", length = 80)
    private String zoneName;

    @Column(nullable = false)
    private boolean serviceable = true;

    @Column(name = "cod_available", nullable = false)
    private boolean codAvailable = true;

    @Column(name = "prepaid_available", nullable = false)
    private boolean prepaidAvailable = true;

    @Column(name = "delivery_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    @Column(name = "free_delivery_threshold", precision = 10, scale = 2)
    private BigDecimal freeDeliveryThreshold;

    @Column(name = "min_delivery_days", nullable = false)
    private Integer minDeliveryDays = 1;

    @Column(name = "max_delivery_days", nullable = false)
    private Integer maxDeliveryDays = 5;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 255)
    private String notes;
}
