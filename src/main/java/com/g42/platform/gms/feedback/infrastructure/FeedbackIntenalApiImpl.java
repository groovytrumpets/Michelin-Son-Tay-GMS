package com.g42.platform.gms.feedback.infrastructure;

import com.g42.platform.gms.feedback.api.internal.FeedbackInternalApi;
import com.g42.platform.gms.feedback.infrastructure.repository.FeedbackJpaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FeedbackIntenalApiImpl implements FeedbackInternalApi {
    @Autowired
    private FeedbackJpaRepo feedbackJpaRepo;
}
