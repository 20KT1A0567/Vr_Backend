package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    List<StockTransfer> findTop100ByOrderByCreatedAtDescIdDesc();
    List<StockTransfer> findByProductIdOrderByCreatedAtDescIdDesc(Long productId);
}
