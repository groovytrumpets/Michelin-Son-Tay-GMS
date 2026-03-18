package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionTireJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafetyInspectionTireRepository extends JpaRepository<SafetyInspectionTireJpa, Integer> {
    
    List<SafetyInspectionTireJpa> findByInspectionId(Integer inspectionId);
    
    @Modifying
    @Query("DELETE FROM SafetyInspectionTireJpa t WHERE t.inspectionId = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Integer inspectionId);
}
