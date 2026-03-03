package com.g42.platform.gms.booking_management.infrastructure.specification;

import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingRequestSpecification {
    public static Specification<BookingRequestJpa> filter(LocalDate date,
                                                          Boolean isGuest,
                                                          BookingRequestStatus status) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (date != null) {
                predicates.add(
                        cb.equal(root.get("scheduledDate"), date)
                );
            }

            if (isGuest != null) {
                predicates.add(
                        cb.equal(root.get("isGuest"), isGuest)
                );
            }

            if (status != null) {
                predicates.add(
                        cb.equal(root.get("status"), status)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
