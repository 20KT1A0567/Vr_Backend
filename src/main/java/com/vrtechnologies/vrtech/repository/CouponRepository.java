package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Optional<Coupon> findByCodeIgnoreCase(String code);
}
