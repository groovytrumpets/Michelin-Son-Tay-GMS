package com.g42.platform.gms.marketing.landingpage.infrastructure.repository;

import com.g42.platform.gms.marketing.landingpage.domain.LandingPageSection;
import com.g42.platform.gms.marketing.landingpage.infrastructure.entity.LandingPageSectionConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandingPageSectionConfigJpaRepo extends JpaRepository<LandingPageSectionConfigJpa, LandingPageSection> {
}
