package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Long> {
    Optional<Attribute> findByNameIgnoreCase(String name);
}
