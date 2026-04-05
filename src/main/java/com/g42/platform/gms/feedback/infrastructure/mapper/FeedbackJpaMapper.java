package com.g42.platform.gms.feedback.infrastructure.mapper;

import com.g42.platform.gms.feedback.domain.entity.Feedback;
import com.g42.platform.gms.feedback.infrastructure.entity.FeedbackJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeedbackJpaMapper {

    Feedback toDomain(FeedbackJpa feedbackJpa);
}
