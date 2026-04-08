package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.EstimateJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateRepositoryJpa;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class EstimateRepositoryImpl implements EstimateRepository {
    private final EstimateRepositoryJpa estimateRepositoryJpa;
    private final EstimateJpaMapper estimateJpaMapper;

    @Override
    public List<Estimate> getListOfEstimateByServiceTiketCode(Integer serviceTicketId) {
        List<EstimateJpa> estimateList = estimateRepositoryJpa.getEstimateJpaByServiceTicketId(serviceTicketId);
        return estimateList.stream().map(estimateJpaMapper::toDomain).toList();
    }

    @Override
    public Estimate save(Estimate estimate) {
        EstimateJpa estimateJpa = estimateJpaMapper.toJpa(estimate);
        estimateRepositoryJpa.save(estimateJpa);

        return estimateRepositoryJpa.findById(estimateJpa.getId()).map(estimateJpaMapper::toDomain).orElse(null);
    }

    @Override
    public Estimate findEstimateById(Integer estimateId) {
        return estimateRepositoryJpa.findById(estimateId).map(estimateJpaMapper::toDomain).orElse(null);
    }

    @Override
    public Estimate findEstimateByServiceIdAndLatestVerson(Integer serviceTicketId) {
        return estimateRepositoryJpa.findTopByServiceTicketIdOrderByVersionDesc(serviceTicketId).map(estimateJpaMapper::toDomain).orElse(null);
    }

    @Override
    public int findLatestEstimate(Integer serviceTicketId) {
        int currentEstimateVersion = estimateRepositoryJpa.findLatestEstimate(serviceTicketId);
        return currentEstimateVersion+1;
    }

    @Override
    public Integer findEstimateIdByVersionAndServiceTicket(Integer serviceTicketId, int latestEstimateVersion) {
        Integer estimateId = estimateRepositoryJpa.findRevisedEstimateId(serviceTicketId,latestEstimateVersion);
        return estimateId;
    }
}

