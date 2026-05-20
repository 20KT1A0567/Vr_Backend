package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.PriceDropAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceDropAlertRepository extends JpaRepository<PriceDropAlert, Long> {
    List<PriceDropAlert> findTop200ByStatusOrderByCreatedAtAscIdAsc(String status);
    List<PriceDropAlert> findTop100ByOrderByCreatedAtDescIdDesc();
}
