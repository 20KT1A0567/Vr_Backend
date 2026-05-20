package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SystemHealthResponse {

    private String status;
    private LocalDateTime checkedAt;
    private List<Component> components;

    @Getter
    @Builder
    public static class Component {
        private String key;
        private String label;
        private String status;
        private String message;
    }
}
