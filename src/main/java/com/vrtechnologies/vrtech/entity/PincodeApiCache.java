package com.vrtechnologies.vrtech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "pincode_api_cache")
public class PincodeApiCache {

    @Id
    @Column(nullable = false, length = 6)
    private String pincode;

    @Column(name = "state_name", nullable = false, length = 80)
    private String stateName;

    @Column(name = "district_name", nullable = false, length = 120)
    private String districtName;

    @Column(name = "city_name", nullable = false, length = 120)
    private String cityName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
