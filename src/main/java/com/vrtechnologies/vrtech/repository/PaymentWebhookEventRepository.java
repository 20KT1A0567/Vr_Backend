package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    List<PaymentWebhookEvent> findTop200ByOrderByCreatedAtDescIdDesc();
    Optional<PaymentWebhookEvent> findByGatewayEventId(String gatewayEventId);
}
