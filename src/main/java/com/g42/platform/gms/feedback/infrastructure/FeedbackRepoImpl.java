package com.g42.platform.gms.feedback.infrastructure;

import com.g42.platform.gms.feedback.domain.repository.FeedbackRepo;
import com.g42.platform.gms.feedback.infrastructure.mapper.FeedbackJpaMapper;
import com.g42.platform.gms.feedback.infrastructure.repository.FeedbackJpaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FeedbackRepoImpl implements FeedbackRepo {
    @Autowired
    private FeedbackJpaRepo feedbackJpaRepo;
    @Autowired
    private FeedbackJpaMapper feedbackJpaMapper;
}
