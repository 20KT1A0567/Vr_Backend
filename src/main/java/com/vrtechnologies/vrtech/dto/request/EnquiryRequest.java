package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnquiryRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    private String email;
    private Long productId;
    private String message;
}
