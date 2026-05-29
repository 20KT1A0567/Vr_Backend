package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;

import java.util.List;

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
}
