package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findTop100ByOrderByCreatedAtDescIdDesc();
    List<StockMovement> findByProductIdOrderByCreatedAtDescIdDesc(Long productId);
}
