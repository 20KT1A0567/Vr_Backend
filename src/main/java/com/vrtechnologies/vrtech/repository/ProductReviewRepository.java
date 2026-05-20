package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findAllByOrderByUpdatedAtDescIdDesc();
}
