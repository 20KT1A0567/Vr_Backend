package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardOrderStatusResponse {

    private String status;
    private long count;
    private double percentage;
}
