package com.g42.platform.gms.feedback.infrastructure;

import com.g42.platform.gms.feedback.domain.entity.Feedback;
import com.g42.platform.gms.feedback.domain.repository.FeedbackRepo;
import com.g42.platform.gms.feedback.infrastructure.entity.FeedbackJpa;
import com.g42.platform.gms.feedback.infrastructure.mapper.FeedbackJpaMapper;
import com.g42.platform.gms.feedback.infrastructure.repository.FeedbackJpaRepo;
import com.g42.platform.gms.feedback.infrastructure.spectification.FeedbackSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class FeedbackRepoImpl implements FeedbackRepo {
    @Autowired
    private FeedbackJpaRepo feedbackJpaRepo;
    @Autowired
    private FeedbackJpaMapper feedbackJpaMapper;

    @Override
    public Page<Feedback> getListOfFeedback(int page, int size, String search, Integer starRating, LocalDateTime start, LocalDateTime end) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification <FeedbackJpa> specification = Specification.unrestricted();
        specification = specification.and(FeedbackSpecification.filterFeedback(starRating,start,end));
        if (search != null && !search.trim().isEmpty()) {
            specification = specification.and((root, query, cb) -> {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                return cb.or(
                        cb.like(cb.lower(root.get("comment").as(String.class)), searchPattern),
                        cb.like(cb.lower(root.get("detailFeedback").as(String.class)), searchPattern)
                );
            });
        }
        return feedbackJpaRepo.findAll(specification,pageable).map(feedbackJpaMapper::toDomain);
    }
}

