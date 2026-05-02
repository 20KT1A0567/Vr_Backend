package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AdminStoreAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminStoreAccessRepository extends JpaRepository<AdminStoreAccess, Long> {

    List<AdminStoreAccess> findByAdminId(Long adminId);

    void deleteByAdminId(Long adminId);

    boolean existsByAdminIdAndStoreId(Long adminId, Long storeId);
}
