package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;

import java.util.Arrays;
import java.util.List;

public interface WorkCategoryRepository {
    List<WorkCategory> findAllById(Iterable<Integer> workCategoryId);
}
