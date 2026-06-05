package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {
    Optional<TrustedDevice> findByDeviceHash(String deviceHash);
    void deleteByExpiresAtBefore(LocalDateTime now);
    void deleteByUser_Id(Long userId);
}
