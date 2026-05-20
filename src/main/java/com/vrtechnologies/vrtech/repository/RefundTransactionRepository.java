package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {
    List<RefundTransaction> findByOrderIdOrderByCreatedAtDescIdDesc(Long orderId);
}
