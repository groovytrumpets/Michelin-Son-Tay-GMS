package com.g42.platform.gms.feedback.api.internal;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackInternalApi {

    void addCusFeedbackRespond(Integer rate, String note, List<String> feedbacks, String trackingId, String submitTime);
}
