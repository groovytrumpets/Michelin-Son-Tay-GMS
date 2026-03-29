package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ProductLineJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface ProductLineJpaRepo extends JpaRepository<ProductLineJpa,Integer> {
    ProductLineJpa findByLineName(String lineName);

    ProductLineJpa findByLineNameContainsIgnoreCase(String lineName);
    interface ProductLineProjection {
        Integer getProductLineId();
        String getLineName();
    }
    @Query("select pl.productLineId as productLineId,pl.lineName as lineName from ProductLineJpa pl where pl.productLineId in :lineIds")
    List<ProductLineProjection> findAllLinesByIdsMap(@Param("lineIds") Set<Integer> lineIds);

    default Map<Integer, String> findAllLinesByIds(Set<Integer> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return Map.of();
        }

        List<ProductLineJpaRepo.ProductLineProjection> projections = findAllLinesByIdsMap(brandIds);

        return projections.stream()
                .collect(Collectors.toMap(
                        ProductLineJpaRepo.ProductLineProjection::getProductLineId,
                        ProductLineJpaRepo.ProductLineProjection::getLineName
                ));
    }
}
