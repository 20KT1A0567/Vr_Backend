package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long> {
    Optional<AttributeValue> findByAttributeIdAndValueIgnoreCase(Long attributeId, String value);
    List<AttributeValue> findByAttributeId(Long attributeId);
}
