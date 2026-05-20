package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserIdOrderByDefaultAddressDescUpdatedAtDescIdDesc(Long userId);
    void deleteByUserIdAndId(Long userId, Long id);
}
