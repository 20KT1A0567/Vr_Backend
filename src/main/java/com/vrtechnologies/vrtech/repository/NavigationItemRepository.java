package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.NavigationItem;
import com.vrtechnologies.vrtech.entity.enums.NavigationMenuLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NavigationItemRepository extends JpaRepository<NavigationItem, Long> {
    List<NavigationItem> findAllByOrderByMenuLocationAscSortOrderAscIdAsc();
    List<NavigationItem> findByVisibleTrueOrderByMenuLocationAscSortOrderAscIdAsc();
    List<NavigationItem> findByMenuLocationOrderBySortOrderAscIdAsc(NavigationMenuLocation menuLocation);
}
