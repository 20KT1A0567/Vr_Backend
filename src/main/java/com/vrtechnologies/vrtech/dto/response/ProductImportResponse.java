package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductImportResponse {
    private int created;
    private int updated;
    private int skipped;
    private List<String> messages;
}
