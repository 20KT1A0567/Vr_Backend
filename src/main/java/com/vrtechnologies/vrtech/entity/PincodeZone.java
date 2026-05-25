package com.vrtechnologies.vrtech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "pincode_zones")
public class PincodeZone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_type", nullable = false, length = 32)
    private String matchType; // PINCODE_PREFIX, DISTRICT, STATE

    @Column(name = "match_value", nullable = false, length = 120)
    private String matchValue;

    @Column(name = "zone_name", nullable = false, length = 80)
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

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false)
    private boolean active = true;
}
