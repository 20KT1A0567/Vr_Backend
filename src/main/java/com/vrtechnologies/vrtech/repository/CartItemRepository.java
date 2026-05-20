package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByOrderByIdDesc();
    List<CartItem> findByUserId(Long userId);
    List<CartItem> findByUserIdOrderByIdAsc(Long userId);
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    List<CartItem> findAllByUserIdAndProductIdOrderByIdAsc(Long userId, Long productId);
    void deleteByUserId(Long userId);
}
