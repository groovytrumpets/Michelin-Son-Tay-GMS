package com.g42.platform.gms.marketing.siteheader.infrastructure.repository;

import com.g42.platform.gms.marketing.siteheader.infrastructure.entity.SiteHeaderConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteHeaderConfigJpaRepo extends JpaRepository<SiteHeaderConfigJpa, Integer> {

    /** Mỗi vị trí chỉ có đúng một dòng — ràng buộc UNIQUE trên location_code. */
    Optional<SiteHeaderConfigJpa> findByLocationCode(String locationCode);
}
