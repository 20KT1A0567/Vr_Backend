package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AdminBackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminBackupCodeRepository extends JpaRepository<AdminBackupCode, Long> {

    List<AdminBackupCode> findByUserIdOrderByCreatedAtAsc(Long userId);

    @Query("select count(c) from AdminBackupCode c where c.userId = :userId and c.consumedAt is null")
    long countActive(@Param("userId") Long userId);

    @Modifying
    @Query("delete from AdminBackupCode c where c.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
