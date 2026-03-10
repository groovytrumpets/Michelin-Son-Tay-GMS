package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.domain.repository.WorkCategoryRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.WorkCategoryJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.WorkCategoryRepositoryJpa;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class WorkCategoryRepositoryImpl implements WorkCategoryRepository {
    private final WorkCategoryRepositoryJpa workCategoryRepositoryJpa;
    private final WorkCategoryJpaMapper workCategoryJpaMapper;

    @Override
    public List<WorkCategory> findAllById(Iterable<Integer> workCategoryId) {
        List<WorkCategoryJpa> workCategoryJpas = workCategoryRepositoryJpa.findAllById(workCategoryId);
        return workCategoryJpas.stream().map(workCategoryJpaMapper::toDomain).toList();
    }

    @Override
    public WorkCategory save(WorkCategory newCategory) {
        WorkCategoryJpa workCategoryJpa = workCategoryJpaMapper.toJpa(newCategory);
        workCategoryRepositoryJpa.save(workCategoryJpa);

        return workCategoryRepositoryJpa.findById(workCategoryJpa.getId()).map(workCategoryJpaMapper::toDomain).orElse(null);
    }
}

