package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.ProductSection;
import com.vrtechnologies.vrtech.entity.enums.ProductSectionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSectionRepository extends JpaRepository<ProductSection, Long> {
    List<ProductSection> findAllByOrderByDisplayOrderAscIdAsc();
    List<ProductSection> findByActiveTrueOrderByDisplayOrderAscIdAsc();
    Optional<ProductSection> findFirstBySectionTypeOrderByDisplayOrderAscIdAsc(ProductSectionType sectionType);
}
