package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.CustomerOrder;
import com.vrtechnologies.vrtech.entity.enums.OrderStatus;
import com.vrtechnologies.vrtech.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CustomerOrder> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    List<CustomerOrder> findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus paymentStatus);
    long countByPaymentStatus(PaymentStatus paymentStatus);
    Optional<CustomerOrder> findByOrderNumberIgnoreCase(String orderNumber);
    long countByStatus(OrderStatus status);
    boolean existsByOrderNumber(String orderNumber);
    boolean existsByInvoiceNumber(String invoiceNumber);
}
