package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreSummaryResponse {

    private Long id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String phone;
    private String whatsapp;
    private String timings;
    private String mapLink;
    private String imageUrl;
    private String videoUrl;
    private boolean active;
}
