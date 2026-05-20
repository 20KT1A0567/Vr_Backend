package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

    @Query("select s from AuthSession s where s.userId = :userId and s.revokedAt is null and s.logoutAt is null and s.refreshTokenExpiresAt > :now order by s.lastUsedAt desc")
    List<AuthSession> findActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update AuthSession s set s.revokedAt = :now, s.logoutAt = :now where s.userId = :userId and s.revokedAt is null and s.id <> :exceptId")
    int revokeAllExcept(@Param("userId") Long userId, @Param("exceptId") Long exceptId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update AuthSession s set s.revokedAt = :now, s.logoutAt = :now where s.userId = :userId and s.revokedAt is null")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
