package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstimateRepositoryJpa extends JpaRepository<EstimateJpa, Integer> {
    List<EstimateJpa> getEstimateJpaByServiceTicketId(Integer serviceTicketId);
}
