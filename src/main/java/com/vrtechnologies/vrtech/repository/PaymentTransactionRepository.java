package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDescIdDesc(Long orderId);
    Optional<PaymentTransaction> findFirstByOrderIdOrderByCreatedAtDescIdDesc(Long orderId);
    Optional<PaymentTransaction> findFirstByGatewayOrderIdOrderByCreatedAtDescIdDesc(String gatewayOrderId);
    Optional<PaymentTransaction> findFirstByGatewayPaymentIdOrderByCreatedAtDescIdDesc(String gatewayPaymentId);
}
