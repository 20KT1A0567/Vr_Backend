package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.OrderTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderTimelineEventRepository extends JpaRepository<OrderTimelineEvent, Long> {
    List<OrderTimelineEvent> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
}
