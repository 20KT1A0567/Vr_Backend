package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaUploadResponse {

    private String url;
    private String publicId;
    private String resourceType;
}
