package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AdminActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog, Long> {

    Page<AdminActivityLog> findByAdminId(Long adminId, Pageable pageable);

    Page<AdminActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
