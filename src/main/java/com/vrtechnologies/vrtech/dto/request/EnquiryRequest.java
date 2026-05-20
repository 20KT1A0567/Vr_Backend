package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EnquiryRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    private String email;
    private String enquiryType;
    private String companyName;
    private Integer quantity;
    private BigDecimal budget;
    private Long productId;
    private String message;
}
