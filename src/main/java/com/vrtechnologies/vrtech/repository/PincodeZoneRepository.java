package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.PincodeZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PincodeZoneRepository extends JpaRepository<PincodeZone, Long> {
    List<PincodeZone> findAllByOrderByPriorityAscIdAsc();
    List<PincodeZone> findByActiveTrueOrderByPriorityAscIdAsc();
}
