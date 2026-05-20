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

@Getter
@Setter
@Entity
@Table(name = "user_addresses")
public class UserAddress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(nullable = false)
    private String contactName;

    @Column(nullable = false)
    private String contactPhone;

    private String contactEmail;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    private String city;

    private String state;

    private String postalCode;

    @Column(nullable = false)
    private boolean defaultAddress;
}
