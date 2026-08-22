package com.g42.platform.gms.marketing.landingpage.infrastructure.entity;

import com.g42.platform.gms.marketing.landingpage.domain.LandingPageSection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "landing_page_section_config")
public class LandingPageSectionConfigJpa {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "section_code", length = 30, nullable = false)
    private LandingPageSection sectionCode;

    @Column(name = "is_configured", nullable = false)
    private Boolean configured = false;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
