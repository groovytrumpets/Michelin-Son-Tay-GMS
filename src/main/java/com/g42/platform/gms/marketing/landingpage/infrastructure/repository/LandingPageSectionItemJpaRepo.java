package com.g42.platform.gms.marketing.landingpage.infrastructure.repository;

import com.g42.platform.gms.marketing.landingpage.domain.LandingPageSection;
import com.g42.platform.gms.marketing.landingpage.infrastructure.entity.LandingPageSectionItemJpa;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandingPageSectionItemJpaRepo extends JpaRepository<LandingPageSectionItemJpa, Integer> {
    @EntityGraph(attributePaths = "catalogItem")
    List<LandingPageSectionItemJpa> findAllByOrderBySectionCodeAscDisplayOrderAsc();

    void deleteAllBySectionCode(LandingPageSection sectionCode);
}
