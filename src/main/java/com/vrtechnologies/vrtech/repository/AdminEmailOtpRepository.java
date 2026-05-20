package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AdminEmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AdminEmailOtpRepository extends JpaRepository<AdminEmailOtp, Long> {

    Optional<AdminEmailOtp> findByChallengeId(String challengeId);

    @Modifying
    @Query("delete from AdminEmailOtp o where o.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("delete from AdminEmailOtp o where o.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
