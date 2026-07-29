package com.g42.platform.gms.bugreport.specification;

import com.g42.platform.gms.bugreport.entity.BugReportJpa;
import com.g42.platform.gms.bugreport.enums.BugReportSeverity;
import com.g42.platform.gms.bugreport.enums.BugReportStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BugReportSpecification {

    /** Trạng thái được coi là "chưa đóng" khi thống kê phiếu nghiêm trọng còn tồn. */
    public static final List<BugReportStatus> OPEN_STATUSES =
            List.of(BugReportStatus.NEW, BugReportStatus.ACKNOWLEDGED, BugReportStatus.IN_PROGRESS);

    public static Specification<BugReportJpa> filter(LocalDateTime start, LocalDateTime end,
                                                     String status, String severity,
                                                     String category, String module,
                                                     String reporterType, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (start != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            if (end != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            addEnumEqual(predicates, cb, root.get("status"), status, BugReportStatus.class);
            addEnumEqual(predicates, cb, root.get("severity"), severity,
                    com.g42.platform.gms.bugreport.enums.BugReportSeverity.class);
            addEnumEqual(predicates, cb, root.get("category"), category,
                    com.g42.platform.gms.bugreport.enums.BugReportCategory.class);
            addEnumEqual(predicates, cb, root.get("reporterType"), reporterType,
                    com.g42.platform.gms.bugreport.enums.ReporterType.class);
            if (module != null && !module.isBlank()) predicates.add(cb.equal(root.get("module"), module));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("reporterName")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<BugReportJpa> withStatus(Specification<BugReportJpa> base, BugReportStatus status) {
        return base.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }

    public static Specification<BugReportJpa> withStatusIn(Specification<BugReportJpa> base, List<BugReportStatus> statuses) {
        return base.and((root, query, cb) -> root.get("status").in(statuses));
    }

    public static Specification<BugReportJpa> withSeverity(Specification<BugReportJpa> base, BugReportSeverity severity) {
        return base.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
    }

    /**
     * Bỏ qua giá trị lọc không thuộc enum thay vì ném lỗi 500 — tham số này đến
     * thẳng từ query string nên không thể tin tưởng.
     */
    private static <E extends Enum<E>> void addEnumEqual(List<Predicate> predicates,
                                                         jakarta.persistence.criteria.CriteriaBuilder cb,
                                                         jakarta.persistence.criteria.Path<?> path,
                                                         String rawValue, Class<E> enumType) {
        if (rawValue == null || rawValue.isBlank()) return;
        try {
            predicates.add(cb.equal(path, Enum.valueOf(enumType, rawValue.trim().toUpperCase())));
        } catch (IllegalArgumentException ignored) {
            // giá trị lạ → coi như không lọc theo tiêu chí này
        }
    }
}
