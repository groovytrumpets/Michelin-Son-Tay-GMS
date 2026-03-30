package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.api.dto.SpecificationRespondDto;
import com.g42.platform.gms.warehouse.infrastructure.entity.ProductLineJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.SpecificationJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpecificationJpaRepo extends JpaRepository<SpecificationJpa,Integer> {
    List<SpecificationJpa> findAllByItemId(Integer itemId);
    interface SpecDetailProjection {
        String getAttributeCode();
        String getDisplayName();
        String getSpecValue();
        String getUnit();
    }
    @Query("""
    SELECT new com.g42.platform.gms.warehouse.api.dto.SpecificationRespondDto(
        sa.attributeCode, 
        sa.displayName, 
        s.specValue, 
        sa.unit
    ) 
    FROM SpecificationJpa s 
    JOIN SpecAttributeJpa sa ON s.attributeId = sa.attributeId 
    WHERE s.itemId = :itemId
""")
    List<SpecificationRespondDto> findSpecsByItemId(@Param("itemId") Integer itemId);

    SpecificationJpa findSpecByItemId(Integer itemId);
}
