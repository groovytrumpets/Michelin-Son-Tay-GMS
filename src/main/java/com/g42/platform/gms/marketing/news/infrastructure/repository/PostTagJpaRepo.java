package com.g42.platform.gms.marketing.news.infrastructure.repository;

import com.g42.platform.gms.marketing.news.infrastructure.entity.PostTagJpa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostTagJpaRepo extends JpaRepository<PostTagJpa, Integer> {

    Optional<PostTagJpa> findBySlug(String slug);

    List<PostTagJpa> findByOrderByUsageCountDescNameAsc(Pageable pageable);
}
