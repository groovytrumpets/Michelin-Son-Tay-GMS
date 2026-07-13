package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.FallbackPricingConfigJpa;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FallbackPricingConfigJpaRepo extends JpaRepository<FallbackPricingConfigJpa, Integer> {

    @Query("""
        select c from FallbackPricingConfigJpa c
        where (:isActive is null or c.isActive = :isActive)
            and (:search is null or lower(c.name) like lower(concat('%', :search, '%')))
    """)
    Page<FallbackPricingConfigJpa> search(
            @Param("isActive") Boolean isActive,
            @Param("search") String search,
            Pageable pageable);

    Optional<FallbackPricingConfigJpa> findFirstByItemTypeAndIsActiveTrue(CatalogItemType itemType);

    Optional<FallbackPricingConfigJpa> findFirstByItemTypeIsNullAndIsActiveTrue();
}
