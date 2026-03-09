package com.g42.platform.gms.marketing.service_catalog.infrastructure.repository;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.CatalogItemJpaEntityF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogRepoJpa extends JpaRepository<CatalogItemJpaEntityF, Integer> {
    @Query("""
       SELECT c.itemId
       FROM CatalogItemJpaEntityF c
       WHERE c.serviceService IN :serviceIds
       """)
    List<Long> findCatalogIdsByServiceIds(@Param("serviceIds") List<Long> serviceIds);
}
