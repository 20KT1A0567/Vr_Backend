package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.CouponStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CouponRequest {

    @NotBlank
    private String code;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal discount;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal minOrder;

    private LocalDate expiryDate;

    @NotNull
    @Min(0)
    private Integer usageLimit;

    @NotNull
    private CouponStatus status;
}
