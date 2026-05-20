package com.vrtechnologies.vrtech.repository;

import com.vrtechnologies.vrtech.entity.SeoSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeoSettingRepository extends JpaRepository<SeoSetting, Long> {
    List<SeoSetting> findAllByOrderByTargetTypeAscTargetSlugAscTargetIdAsc();
    Optional<SeoSetting> findFirstByTargetTypeAndTargetId(String targetType, Long targetId);
    Optional<SeoSetting> findFirstByTargetTypeAndTargetSlugIgnoreCase(String targetType, String targetSlug);
    Optional<SeoSetting> findFirstByTargetTypeAndTargetIdIsNullAndTargetSlugIsNull(String targetType);
}
