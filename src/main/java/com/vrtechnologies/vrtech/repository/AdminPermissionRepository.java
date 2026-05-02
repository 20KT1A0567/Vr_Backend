package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AdminPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminPermissionRepository extends JpaRepository<AdminPermission, Long> {

    List<AdminPermission> findByAdminId(Long adminId);

    void deleteByAdminId(Long adminId);
}
