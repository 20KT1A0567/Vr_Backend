package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AttributeResponse {
    private Long id;
    private String name;
    private List<AttributeValueResponse> values;

    @Getter
    @Builder
    public static class AttributeValueResponse {
        private Long id;
        private String value;
    }
}
