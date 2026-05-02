package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.OrderTimelineEventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderTimelineEventResponse {

    private Long id;
    private OrderTimelineEventType eventType;
    private String title;
    private String description;
    private String source;
    private Long actorId;
    private String actorName;
    private String actorEmail;
    private LocalDateTime createdAt;
}
