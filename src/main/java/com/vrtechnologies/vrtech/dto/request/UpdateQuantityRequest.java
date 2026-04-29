package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateQuantityRequest {

    @Min(1)
    private Integer quantity;
}
