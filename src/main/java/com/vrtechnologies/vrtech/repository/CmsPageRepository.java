package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.CmsPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CmsPageRepository extends JpaRepository<CmsPage, Long> {
    Optional<CmsPage> findBySlug(String slug);
}
