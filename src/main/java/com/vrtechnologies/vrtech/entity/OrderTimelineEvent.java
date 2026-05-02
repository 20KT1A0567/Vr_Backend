package com.vrtechnologies.vrtech.entity;

import com.vrtechnologies.vrtech.config.JsonMapConverter;
import com.vrtechnologies.vrtech.entity.enums.OrderTimelineEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "order_timeline_events")
public class OrderTimelineEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrderTimelineEventType eventType;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 64)
    private String source;

    private Long actorId;

    @Column(length = 120)
    private String actorName;

    @Column(length = 160)
    private String actorEmail;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "LONGTEXT")
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
