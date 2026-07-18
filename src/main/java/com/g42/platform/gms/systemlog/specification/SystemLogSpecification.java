package com.g42.platform.gms.systemlog.specification;

import com.g42.platform.gms.systemlog.entity.SystemLogJpa;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SystemLogSpecification {

    public static Specification<SystemLogJpa> filter(LocalDateTime start, LocalDateTime end,
                                                     String role, String action,
                                                     String severity, String module,
                                                     String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            if (end != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            if (role != null && !role.isBlank()) predicates.add(cb.equal(root.get("actorRole"), role));
            if (action != null && !action.isBlank()) predicates.add(cb.equal(root.get("action"), action));
            if (severity != null && !severity.isBlank()) predicates.add(cb.equal(root.get("severity"), severity));
            if (module != null && !module.isBlank()) predicates.add(cb.equal(root.get("module"), module));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("actorName")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("targetId")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<SystemLogJpa> withAction(Specification<SystemLogJpa> base, String actionCode) {
        return base.and((root, query, cb) -> cb.equal(root.get("action"), actionCode));
    }

    public static Specification<SystemLogJpa> withActionIn(Specification<SystemLogJpa> base, List<String> actionCodes) {
        return base.and((root, query, cb) -> root.get("action").in(actionCodes));
    }
}
