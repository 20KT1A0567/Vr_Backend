package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StoreRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    private String landmark;

    private String postalCode;

    @NotBlank
    private String city;

    private String state;

    @NotBlank
    private String phone;

    private String whatsapp;
    private String timings;
    private String mapLink;
    private String imageUrl;
    private String videoUrl;
    private BigDecimal googleRating;
    private Integer googleReviewCount;
    private Boolean active;
}
