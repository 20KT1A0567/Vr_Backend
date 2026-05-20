package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.Enquiry;
import com.vrtechnologies.vrtech.entity.enums.EnquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    long countByStatus(EnquiryStatus status);
    List<Enquiry> findByEmailIgnoreCaseOrderByCreatedAtDescIdDesc(String email);
    List<Enquiry> findByPhoneOrderByCreatedAtDescIdDesc(String phone);
}
