package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AdminActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog, Long> {

    Page<AdminActivityLog> findByAdminId(Long adminId, Pageable pageable);

    Page<AdminActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select l from AdminActivityLog l where l.entityType = :entityType and l.entityId = :entityId order by l.createdAt desc")
    Page<AdminActivityLog> findByEntity(@Param("entityType") String entityType,
                                        @Param("entityId") Long entityId,
                                        Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime createdAt);
}
