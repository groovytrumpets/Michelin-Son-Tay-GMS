package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkCategoryRepositoryJpa extends JpaRepository<WorkCategoryJpa, Integer> {
    @Query("SELECT COALESCE(MAX(wc.displayOrder), 0) FROM WorkCategoryJpa wc")
    int findMaxDisplayOrder();
    @Query("""
    select wc from WorkCategoryJpa wc where wc.id=:categoryId
        """)
    WorkCategoryJpa findByIdWork(Integer categoryId);

    List<WorkCategoryJpa> findAllByIsDefault(Boolean isDefault);
}
