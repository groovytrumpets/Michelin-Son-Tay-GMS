package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.TicketCustomCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCustomCategoryRepository extends JpaRepository<TicketCustomCategoryJpa, Integer> {

    List<TicketCustomCategoryJpa> findByInspectionId(Integer inspectionId);

    boolean existsByInspectionIdAndCategoryName(Integer inspectionId, String categoryName);
}
