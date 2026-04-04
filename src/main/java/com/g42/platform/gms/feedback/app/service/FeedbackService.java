package com.g42.platform.gms.feedback.app.service;

import com.g42.platform.gms.feedback.api.dto.FeedbackDto;
import com.g42.platform.gms.feedback.domain.entity.Feedback;
import com.g42.platform.gms.feedback.domain.repository.FeedbackRepo;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FeedbackService {
    private final FeedbackRepo feedbackRepo;

    public FeedbackService(FeedbackRepo feedbackRepo) {
        this.feedbackRepo = feedbackRepo;
    }

    public Page<FeedbackDto> getListItems(int page, int size, String search, Integer starRating, LocalDateTime start, LocalDateTime end) {
        Page<Feedback> feedbacks = feedbackRepo.getListOfFeedback(page,size,search,starRating,start,end);
    }
}
