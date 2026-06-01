package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);
    boolean existsBySku(String sku);
    List<ProductVariant> findByProductId(Long productId);
}
