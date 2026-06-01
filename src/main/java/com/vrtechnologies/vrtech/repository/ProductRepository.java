package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findTop8ByFeaturedTrueAndAvailableTrueOrderByUpdatedAtDesc();
    List<Product> findByAvailableTrueOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"brand", "category", "images", "stores"})
    List<Product> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"brand", "category", "images", "stores"})
    @Override
    List<Product> findAll(Specification<Product> spec, Sort sort);

    @EntityGraph(attributePaths = {"brand", "category", "images", "stores"})
    @Override
    List<Product> findAll(Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}

