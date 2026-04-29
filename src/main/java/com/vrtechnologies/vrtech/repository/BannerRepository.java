package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByActiveTrueOrderBySortOrderAscIdDesc();
    List<Banner> findAllByOrderBySortOrderAscIdDesc();
}
