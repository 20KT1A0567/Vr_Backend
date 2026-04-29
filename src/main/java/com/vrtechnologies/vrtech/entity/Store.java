package com.vrtechnologies.vrtech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private String city;

    private String state = "Andhra Pradesh";

    @Column(nullable = false)
    private String phone;

    private String whatsapp;

    private String timings;

    private String mapLink;

    private String imageUrl;

    private String videoUrl;

    @Column(nullable = false)
    private boolean active = true;

    @JsonIgnore
    @ManyToMany(mappedBy = "stores")
    private Set<Product> products = new LinkedHashSet<>();
}
