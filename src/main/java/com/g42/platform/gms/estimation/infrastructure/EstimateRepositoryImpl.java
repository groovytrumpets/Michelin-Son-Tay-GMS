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
}

