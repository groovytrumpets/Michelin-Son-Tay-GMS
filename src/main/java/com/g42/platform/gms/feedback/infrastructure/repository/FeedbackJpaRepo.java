package com.g42.platform.gms.feedback.infrastructure.repository;

import com.g42.platform.gms.feedback.infrastructure.entity.FeedbackJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackJpaRepo extends JpaRepository<FeedbackJpa, Integer>, JpaSpecificationExecutor<FeedbackJpa> {
}
