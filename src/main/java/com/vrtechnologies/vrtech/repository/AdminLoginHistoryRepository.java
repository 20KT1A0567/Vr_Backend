package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AdminLoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLoginHistoryRepository extends JpaRepository<AdminLoginHistory, Long> {

    Page<AdminLoginHistory> findByAdminId(Long adminId, Pageable pageable);

    Page<AdminLoginHistory> findAllByOrderByLoginAtDesc(Pageable pageable);

    AdminLoginHistory findFirstBySessionIdOrderByLoginAtDesc(Long sessionId);
}
