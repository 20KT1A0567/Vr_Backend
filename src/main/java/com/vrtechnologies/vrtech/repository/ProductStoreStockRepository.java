package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.ProductStoreStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductStoreStockRepository extends JpaRepository<ProductStoreStock, Long> {
    List<ProductStoreStock> findByProductId(Long productId);
    Optional<ProductStoreStock> findByProductIdAndStoreId(Long productId, Long storeId);
    List<ProductStoreStock> findByProductIdIn(List<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pss FROM ProductStoreStock pss WHERE pss.product.id = :productId AND pss.store.id = :storeId")
    Optional<ProductStoreStock> findByProductIdAndStoreIdForUpdate(@Param("productId") Long productId, @Param("storeId") Long storeId);
}

