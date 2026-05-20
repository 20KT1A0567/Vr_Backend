package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.ReturnRequest;
import com.vrtechnologies.vrtech.entity.enums.ReturnRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Optional<ReturnRequest> findFirstByOrderIdOrderByCreatedAtDescIdDesc(Long orderId);
    List<ReturnRequest> findAllByOrderByCreatedAtDescIdDesc();
    List<ReturnRequest> findByStatusOrderByCreatedAtDescIdDesc(ReturnRequestStatus status);
    List<ReturnRequest> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    long countByStatusIn(List<ReturnRequestStatus> statuses);
}
