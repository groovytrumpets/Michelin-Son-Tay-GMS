package com.g42.platform.gms.feedback.domain.repository;

import com.g42.platform.gms.feedback.domain.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FeedbackRepo {
    Page<Feedback> getListOfFeedback(int page, int size, String search, Integer starRating, LocalDateTime start, LocalDateTime end);
}
