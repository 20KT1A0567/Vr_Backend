package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {
    List<ProductPriceHistory> findByProductIdAndCreatedAtAfter(Long productId, LocalDateTime cutoff);
    List<ProductPriceHistory> findByProductIdInAndCreatedAtAfter(List<Long> productIds, LocalDateTime cutoff);
    List<ProductPriceHistory> findByProductId(Long productId);
}
