package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.BrandJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ItemCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCategoryJpaRepo extends JpaRepository<ItemCategoryJpa,Integer> {
    boolean existsByCategoryCode(String categoryCode);

    ItemCategoryJpa findByCategoryCode(String categoryCode);

    ItemCategoryJpa findByCategoryCodeContainingIgnoreCase(String categoryCode);
}
