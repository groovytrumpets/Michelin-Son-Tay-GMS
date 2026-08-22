package com.g42.platform.gms.marketing.landingpage.infrastructure.entity;

import com.g42.platform.gms.marketing.landingpage.domain.LandingPageSection;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "landing_page_section_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_landing_section_catalog_item", columnNames = {"section_code", "catalog_item_id"}),
                @UniqueConstraint(name = "uk_landing_section_display_order", columnNames = {"section_code", "display_order"})
        }
)
public class LandingPageSectionItemJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_code", length = 30, nullable = false)
    private LandingPageSection sectionCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private CatalogItemJpa catalogItem;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
