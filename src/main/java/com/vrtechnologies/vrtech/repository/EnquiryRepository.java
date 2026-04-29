package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.Enquiry;
import com.vrtechnologies.vrtech.entity.enums.EnquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    long countByStatus(EnquiryStatus status);
}
