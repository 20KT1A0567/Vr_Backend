package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminRoleRepository extends JpaRepository<AdminRole, String> {

    List<AdminRole> findAllByOrderBySystemRoleDescDisplayNameAsc();

    boolean existsByRoleKeyIgnoreCase(String roleKey);
}
