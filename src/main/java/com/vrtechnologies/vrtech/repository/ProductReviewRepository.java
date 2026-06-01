package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.ProductReview;
import com.vrtechnologies.vrtech.entity.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findAllByOrderByUpdatedAtDescIdDesc();
    List<ProductReview> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, ReviewStatus status);
}

