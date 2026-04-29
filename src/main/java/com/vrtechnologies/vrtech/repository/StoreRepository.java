package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByActiveTrueOrderByCityAscNameAsc();
    List<Store> findAllByOrderByCityAscNameAsc();
}
