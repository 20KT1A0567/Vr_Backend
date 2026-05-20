package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.ProductStoreStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductStoreStockRepository extends JpaRepository<ProductStoreStock, Long> {
    List<ProductStoreStock> findByProductId(Long productId);
    Optional<ProductStoreStock> findByProductIdAndStoreId(Long productId, Long storeId);
    List<ProductStoreStock> findByProductIdIn(List<Long> productIds);
}
