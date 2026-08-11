package com.g42.platform.gms.marketing.news.infrastructure.repository;

import com.g42.platform.gms.marketing.news.infrastructure.entity.PostViewLogJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PostViewLogJpaRepo extends JpaRepository<PostViewLogJpa, Long> {

    /** Cùng người xem cùng bài trong cửa sổ thời gian ngắn thì không đếm lại. */
    boolean existsByPostIdAndVisitorHashAndViewedAtAfter(Long postId, String visitorHash, LocalDateTime after);
}
