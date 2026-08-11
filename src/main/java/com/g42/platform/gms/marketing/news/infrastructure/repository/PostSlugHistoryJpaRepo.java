package com.g42.platform.gms.marketing.news.infrastructure.repository;

import com.g42.platform.gms.marketing.news.infrastructure.entity.PostSlugHistoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostSlugHistoryJpaRepo extends JpaRepository<PostSlugHistoryJpa, Long> {

    Optional<PostSlugHistoryJpa> findByOldSlug(String oldSlug);

    boolean existsByOldSlug(String oldSlug);

    void deleteByOldSlug(String oldSlug);
}
