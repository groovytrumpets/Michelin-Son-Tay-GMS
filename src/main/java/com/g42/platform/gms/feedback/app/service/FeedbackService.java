package com.g42.platform.gms.feedback.app.service;

import com.g42.platform.gms.feedback.api.dto.FeedbackDto;
import com.g42.platform.gms.feedback.api.mapper.FeedbackDtoMapper;
import com.g42.platform.gms.feedback.domain.entity.Feedback;
import com.g42.platform.gms.feedback.domain.repository.FeedbackRepo;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FeedbackService {
    private final FeedbackRepo feedbackRepo;
    private final FeedbackDtoMapper feedbackDtoMapper;

    public FeedbackService(FeedbackRepo feedbackRepo, FeedbackDtoMapper feedbackDtoMapper) {
        this.feedbackRepo = feedbackRepo;
        this.feedbackDtoMapper = feedbackDtoMapper;
    }

    public Page<FeedbackDto> getListItems(int page, int size, String search, Integer starRating, LocalDateTime start, LocalDateTime end) {
        Page<Feedback> feedbacks = feedbackRepo.getListOfFeedback(page,size,search,starRating,start,end);
        return feedbacks.map(feedbackDtoMapper::toDto);
    }
}
