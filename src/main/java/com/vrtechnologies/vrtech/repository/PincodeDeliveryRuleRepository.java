package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.PincodeDeliveryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PincodeDeliveryRuleRepository extends JpaRepository<PincodeDeliveryRule, Long> {
    List<PincodeDeliveryRule> findAllByOrderByPincodeAscPriorityAscIdAsc();
    List<PincodeDeliveryRule> findByPincodeAndActiveTrueOrderByPriorityAscIdAsc(String pincode);
    Optional<PincodeDeliveryRule> findFirstByPincodeAndStoreIdOrderByIdAsc(String pincode, Long storeId);
    Optional<PincodeDeliveryRule> findFirstByPincodeAndStoreIsNullOrderByIdAsc(String pincode);
}
