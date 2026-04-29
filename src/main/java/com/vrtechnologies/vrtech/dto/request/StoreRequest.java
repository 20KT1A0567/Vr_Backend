package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String address;

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
    private Boolean active;
}
