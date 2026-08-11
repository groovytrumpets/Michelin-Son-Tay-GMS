package com.g42.platform.gms.marketing.news.infrastructure.repository;

import com.g42.platform.gms.marketing.news.infrastructure.entity.PostCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostCategoryJpaRepo extends JpaRepository<PostCategoryJpa, Integer> {

    Optional<PostCategoryJpa> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<PostCategoryJpa> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    List<PostCategoryJpa> findAllByOrderByDisplayOrderAscNameAsc();
}
