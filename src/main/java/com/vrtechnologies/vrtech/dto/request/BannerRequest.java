package com.vrtechnologies.vrtech.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BannerRequest {

    private String title;
    private String subtitle;

    private String imageUrl;

    private String videoUrl;
    private String linkUrl;
    private Boolean active;
    private Integer sortOrder;
}
