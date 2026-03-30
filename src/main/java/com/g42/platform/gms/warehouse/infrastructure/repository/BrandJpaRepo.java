package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.BrandJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface BrandJpaRepo extends JpaRepository<BrandJpa,Integer> {
    BrandJpa findByBrandName(String brandName);
    interface BrandProjection {
        Integer getBrandId();
        String getBrandName();
    }
    BrandJpa findByBrandNameContainingIgnoreCase(String brandName);
    @Query("SELECT b.brandId AS brandId, b.brandName AS brandName FROM BrandJpa b WHERE b.brandId IN :brandIds")
    List<BrandProjection> fetchBrandNamesByIds(@Param("brandIds") Set<Integer> brandIds);

    default Map<Integer, String> getBrandMapByIds(Set<Integer> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return Map.of();
        }

        List<BrandProjection> projections = fetchBrandNamesByIds(brandIds);

        return projections.stream()
                .collect(Collectors.toMap(
                        BrandProjection::getBrandId,
                        BrandProjection::getBrandName
                ));
    }
}
