package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.EstimateItemJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.mapper.EstimateJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateItemRepositoryJpa;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class EstimateItemRepositoryImpl implements EstimateItemRepository {
    private final EstimateItemRepositoryJpa estimateItemRepositoryJpa;
    private final EstimateItemJpaMapper estimateItemJpaMapper;

    @Override
    public List<EstimateItem> findByEstimateIds(List<Integer> estimateIds) {
        List<EstimateItemJpa> list = estimateItemRepositoryJpa.findByEstimateIds(estimateIds);
        return list.stream().map(estimateItemJpaMapper::toDomain).toList();
    }

    @Override
    public void saveAll(List<EstimateItem> items) {
        estimateItemRepositoryJpa.saveAll(items.stream().map(estimateItemJpaMapper::toJpa).toList());
    }

    @Override
    public List<EstimateItem> findByEstimateId(Integer estimateId) {
        List<EstimateItemJpa> list = estimateItemRepositoryJpa.findByEstimateId(estimateId);
        return list.stream().map(estimateItemJpaMapper::toDomain).toList();
    }

    @Override
    public void delete(EstimateItem estimateItem) {
        estimateItemRepositoryJpa.delete(estimateItemJpaMapper.toJpa(estimateItem));
    }
}

