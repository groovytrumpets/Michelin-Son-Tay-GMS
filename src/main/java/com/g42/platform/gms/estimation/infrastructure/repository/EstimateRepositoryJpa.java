package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstimateRepositoryJpa extends JpaRepository<EstimateJpa, Integer> {
    List<EstimateJpa> getEstimateJpaByServiceTicketId(Integer serviceTicketId);

    <T> Optional<EstimateJpa> findTopByIdOrderByVersionDesc(Integer id);

    <T> Optional<EstimateJpa> findTopByServiceTicketIdOrderByVersionDesc(Integer serviceTicketId);

    EstimateJpa findByServiceTicketId(Integer serviceTicketId);
    @Query("""
    select coalesce(max(e.version),0) from EstimateJpa e where e.serviceTicketId =:serviceTicketId
    """)
    int findLatestEstimate(Integer serviceTicketId);
    @Query("""
    SELECT e FROM EstimateJpa e 
    WHERE e.serviceTicketId IN :ticketIds 
      AND e.version = (
          SELECT MAX(e2.version) 
          FROM EstimateJpa e2 
          WHERE e2.serviceTicketId = e.serviceTicketId
      )
""")
    List<EstimateJpa> findByServiceTicketIdsAndVersionTop(List<Integer> ticketIds);
    @Query("""
    select e.id from EstimateJpa  e where e.serviceTicketId = :serviceTicketId and e.version = :latestEstimateVersion
    """)
    Integer findRevisedEstimateId(Integer serviceTicketId, int latestEstimateVersion);

    List<EstimateJpa> findAllByServiceTicketId(Integer serviceTicketId);
}
