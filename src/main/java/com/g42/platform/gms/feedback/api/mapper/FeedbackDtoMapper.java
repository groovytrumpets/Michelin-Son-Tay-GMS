package com.g42.platform.gms.feedback.api.mapper;

import com.g42.platform.gms.feedback.api.dto.FeedbackDto;
import com.g42.platform.gms.feedback.domain.entity.Feedback;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeedbackDtoMapper {
    FeedbackDto toDto(Feedback feedback);
}
