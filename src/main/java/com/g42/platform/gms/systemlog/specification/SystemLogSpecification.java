package com.g42.platform.gms.systemlog.specification;

import com.g42.platform.gms.systemlog.dto.SystemLogFilter;
import com.g42.platform.gms.systemlog.entity.SystemLogJpa;
import com.g42.platform.gms.systemlog.service.SystemLogOperation;
import com.g42.platform.gms.systemlog.service.SystemLogTargetResolver.Kind;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SystemLogSpecification {

    /**
     * @param searchHits id mặt hàng / phiếu có tên hoặc mã khớp chữ tìm kiếm (để tìm được nhật ký cũ
     *                   chỉ lưu id); rỗng nếu không tìm hoặc không khớp gì.
     */
    public static Specification<SystemLogJpa> filter(SystemLogFilter f, Map<Kind, List<Integer>> searchHits) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Expression<String> description = root.get("description");
            Expression<String> targetId = root.get("targetId");

            if (f.getStartDate() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.getStartDate()));
            if (f.getEndDate() != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), f.getEndDate()));
            if (notBlank(f.getRole())) predicates.add(cb.equal(root.get("actorRole"), f.getRole()));
            if (notBlank(f.getAction())) predicates.add(cb.equal(root.get("action"), f.getAction()));
            if (notBlank(f.getSeverity())) predicates.add(cb.equal(root.get("severity"), f.getSeverity()));
            if (notBlank(f.getModule())) predicates.add(cb.equal(root.get("module"), f.getModule()));
            if (f.getActorStaffId() != null) predicates.add(cb.equal(root.get("actorStaffId"), f.getActorStaffId()));

            SystemLogOperation operation = SystemLogOperation.parse(f.getOperation());
            if (operation != null) {
                List<Predicate> any = new ArrayList<>();
                for (String fragment : operation.pathFragments()) any.add(cb.like(description, contains(fragment)));
                if (!operation.modules().isEmpty()) any.add(root.get("module").in(operation.modules()));
                if (!operation.actions().isEmpty()) any.add(root.get("action").in(operation.actions()));
                predicates.add(cb.or(any.toArray(new Predicate[0])));
            }

            if (Boolean.TRUE.equals(f.getHideNoise())) {
                List<Predicate> noise = new ArrayList<>();
                for (String fragment : SystemLogOperation.NOISE_FRAGMENTS) noise.add(cb.like(description, contains(fragment)));
                // description NULL thì NOT LIKE ra NULL và dòng bị loại oan
                predicates.add(cb.or(cb.isNull(description), cb.not(cb.or(noise.toArray(new Predicate[0])))));
            }

            Kind targetKind = Kind.parse(f.getTargetKind());
            if (targetKind != null && notBlank(f.getTargetId())) {
                predicates.add(cb.and(cb.equal(targetId, f.getTargetId().trim()), likeAny(cb, description, targetKind.pathFragments)));
            }

            if (notBlank(f.getSearch())) {
                String like = "%" + f.getSearch().trim().toLowerCase() + "%";
                List<Predicate> any = new ArrayList<>();
                any.add(cb.like(cb.lower(root.get("actorName")), like));
                any.add(cb.like(cb.lower(description), like));
                any.add(cb.like(cb.lower(targetId), like));
                if (searchHits != null) {
                    searchHits.forEach((kind, ids) -> {
                        List<String> idTexts = ids.stream().map(String::valueOf).toList();
                        any.add(cb.and(targetId.in(idTexts), likeAny(cb, description, kind.pathFragments)));
                    });
                }
                predicates.add(cb.or(any.toArray(new Predicate[0])));
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

    private static Predicate likeAny(CriteriaBuilder cb, Expression<String> description, List<String> fragments) {
        List<Predicate> any = new ArrayList<>();
        for (String fragment : fragments) any.add(cb.like(description, contains(fragment)));
        return cb.or(any.toArray(new Predicate[0]));
    }

    /** Đoạn đường dẫn không chứa % hay _ nên không cần escape. */
    private static String contains(String fragment) {
        return "%" + fragment + "%";
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
