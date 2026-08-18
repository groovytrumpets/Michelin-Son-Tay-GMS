package com.g42.platform.gms.marketing.itempost.infrastructure.repository;

import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemPostCategoryJpaRepo extends JpaRepository<ItemPostCategoryJpa, Integer> {

    Optional<ItemPostCategoryJpa> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<ItemPostCategoryJpa> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    List<ItemPostCategoryJpa> findAllByOrderByDisplayOrderAscNameAsc();
}
