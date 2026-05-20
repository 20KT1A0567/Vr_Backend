package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.RecentlyViewedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentlyViewedProductRepository extends JpaRepository<RecentlyViewedProduct, Long> {
    List<RecentlyViewedProduct> findTop20ByUserIdOrderByUpdatedAtDescIdDesc(Long userId);
    List<RecentlyViewedProduct> findTop20ByAnonymousIdOrderByUpdatedAtDescIdDesc(String anonymousId);
    Optional<RecentlyViewedProduct> findFirstByUserIdAndProductId(Long userId, Long productId);
    Optional<RecentlyViewedProduct> findFirstByAnonymousIdAndProductId(String anonymousId, Long productId);
}
