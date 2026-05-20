package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.BackInStockRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackInStockRequestRepository extends JpaRepository<BackInStockRequest, Long> {
    List<BackInStockRequest> findTop200ByOrderByCreatedAtDescIdDesc();
    List<BackInStockRequest> findByProductIdAndStatus(Long productId, String status);
    boolean existsByProductIdAndEmailIgnoreCaseAndStatus(Long productId, String email, String status);
    List<BackInStockRequest> findByEmailIgnoreCaseOrderByCreatedAtDescIdDesc(String email);
}
