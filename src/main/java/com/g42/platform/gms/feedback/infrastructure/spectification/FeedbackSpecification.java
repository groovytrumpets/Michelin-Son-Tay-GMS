package com.g42.platform.gms.feedback.infrastructure.spectification;

import com.g42.platform.gms.feedback.infrastructure.entity.FeedbackJpa;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FeedbackSpecification {
    public static Specification<FeedbackJpa> filterFeedback(Integer starRating, LocalDateTime start, LocalDateTime end) {
        return  (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (starRating != null) predicates.add(cb.equal(root.get("starRating"), starRating));
            if (start != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            if (end != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
