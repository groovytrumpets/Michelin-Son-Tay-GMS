package com.g42.platform.gms.feedback.infrastructure;

import com.g42.platform.gms.feedback.api.internal.FeedbackInternalApi;
import com.g42.platform.gms.feedback.infrastructure.entity.FeedbackJpa;
import com.g42.platform.gms.feedback.infrastructure.repository.FeedbackJpaRepo;
import com.g42.platform.gms.service_ticket_management.api.internal.ServiceTicketInternalApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class FeedbackIntenalApiImpl implements FeedbackInternalApi {
    @Autowired
    private FeedbackJpaRepo feedbackJpaRepo;
    @Autowired
    private ServiceTicketInternalApi serviceTicketInternalApi;

    @Override
    public void addCusFeedbackRespond(Integer rate, String note, List<String> feedbacks, String trackingId, String submitTime) {
        FeedbackJpa feedbackJpa = new FeedbackJpa();
        feedbackJpa.setStarRating(rate);
        feedbackJpa.setComment(note);
        feedbackJpa.setCreatedAt(Instant.now());
        Integer serviceId = serviceTicketInternalApi.getServiceIdByCode(trackingId);
        feedbackJpa.setServiceTicketId(serviceId);
        String joinedFeedbacks = (feedbacks != null && !feedbacks.isEmpty())
                ? String.join(", ", feedbacks)
                : null;
        feedbackJpa.setDetailFeedback(joinedFeedbacks);
        FeedbackJpa savedFeedbackJpa = feedbackJpaRepo.save(feedbackJpa);

    }
}
