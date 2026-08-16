package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.infrastructure.entity.ItemCategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemCategoryRepositoryJpa extends JpaRepository<ItemCategoryJpaEntity, Integer> {

    List<ItemCategoryJpaEntity> findAllByIsActiveTrueOrderByDisplayOrderAscCategoryNameAsc();

    /** Tra danh mục theo tên, không phân biệt hoa thường. */
    @Query("""
        select c from EstimateItemCategory c
        where lower(trim(c.categoryName)) = lower(trim(:categoryName))
    """)
    List<ItemCategoryJpaEntity> findByCategoryNameIgnoreCase(String categoryName);
}
