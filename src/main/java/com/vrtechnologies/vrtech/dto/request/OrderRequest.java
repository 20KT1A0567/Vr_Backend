package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.DeliveryType;
import com.vrtechnologies.vrtech.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {

    @NotNull
    private DeliveryType deliveryType;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    private Long storeId;

    @NotBlank
    private String contactName;

    @NotBlank
    private String contactPhone;

    private String contactEmail;
    private String deliveryAddress;
    private String notes;
}
