package com.vrtechnologies.vrtech.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminStoresRequest {

    private List<Long> storeIds;
}
