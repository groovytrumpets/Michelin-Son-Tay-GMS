package com.g42.platform.gms.marketing.itempost.infrastructure.repository;

import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostSlugHistoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemPostSlugHistoryJpaRepo extends JpaRepository<ItemPostSlugHistoryJpa, Long> {

    Optional<ItemPostSlugHistoryJpa> findByOldSlug(String oldSlug);

    boolean existsByOldSlug(String oldSlug);

    void deleteByOldSlug(String oldSlug);
}
