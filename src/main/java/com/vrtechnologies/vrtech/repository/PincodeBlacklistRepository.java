package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.PincodeBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PincodeBlacklistRepository extends JpaRepository<PincodeBlacklist, Long> {
    List<PincodeBlacklist> findAllByOrderByPincodeAsc();
    Optional<PincodeBlacklist> findByPincodeAndActiveTrue(String pincode);
}
