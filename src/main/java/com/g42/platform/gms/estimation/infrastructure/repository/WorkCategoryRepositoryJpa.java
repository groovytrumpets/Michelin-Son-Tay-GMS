package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkCategoryRepositoryJpa extends JpaRepository<WorkCategoryJpa, Integer> {
    @Query("SELECT COALESCE(MAX(wc.displayOrder), 0) FROM WorkCategoryJpa wc")
    int findMaxDisplayOrder();
}
