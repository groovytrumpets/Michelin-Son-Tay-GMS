package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.infrastructure.mapper.EstimateJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateRepositoryJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstimateInternalApiImpl implements EstimateInternalApi {
    @Autowired
    private EstimateRepositoryJpa estimateRepositoryJpa;
    @Autowired
    private EstimateJpaMapper estimateJpaMapper;

    @Override
    public List<Estimate> findAllByServiceTicketId(List<Integer> ticketIds) {
        return estimateRepositoryJpa.findByServiceTicketIdsAndVersionTop(ticketIds).stream().map(estimateJpaMapper::toDomain).toList();
    }
}
