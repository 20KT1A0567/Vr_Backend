package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.OrderTimelineEvent;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.OrderTimelineEventType;
import com.vrtechnologies.vrtech.repository.OrderTimelineEventRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderTimelineService {

    private final OrderTimelineEventRepository repository;

    public OrderTimelineService(OrderTimelineEventRepository repository) {
        this.repository = repository;
    }

    public void record(CustomerOrder order, OrderTimelineEventType eventType, String title, String description, User actor, String source) {
        record(order, eventType, title, description, actor, source, Map.of());
    }

    public void record(CustomerOrder order, OrderTimelineEventType eventType, String title, String description, User actor, String source, Map<String, Object> metadata) {
        OrderTimelineEvent event = new OrderTimelineEvent();
        event.setOrder(order);
        event.setEventType(eventType);
        event.setTitle(title);
        event.setDescription(description);
        event.setSource(source);
        if (actor != null) {
            event.setActorId(actor.getId());
            event.setActorName(actor.getName());
            event.setActorEmail(actor.getEmail());
        }
        event.setMetadata(new LinkedHashMap<>(metadata));
        repository.save(event);
    }

    public List<OrderTimelineEvent> findForOrder(Long orderId) {
        return repository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId);
    }
}
