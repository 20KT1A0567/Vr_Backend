package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.PincodeLookupLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PincodeLookupLogRepository extends JpaRepository<PincodeLookupLog, Long> {
    List<PincodeLookupLog> findTop100ByOrderByCreatedAtDesc();
}
