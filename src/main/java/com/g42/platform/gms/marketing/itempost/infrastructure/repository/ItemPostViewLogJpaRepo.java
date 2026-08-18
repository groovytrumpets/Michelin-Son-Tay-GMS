package com.g42.platform.gms.marketing.itempost.infrastructure.repository;

import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostViewLogJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ItemPostViewLogJpaRepo extends JpaRepository<ItemPostViewLogJpa, Long> {

    /** Cùng người xem cùng bài trong cửa sổ thời gian ngắn thì không đếm lại. */
    boolean existsByItemPostIdAndVisitorHashAndViewedAtAfter(Long itemPostId, String visitorHash, LocalDateTime after);
}
