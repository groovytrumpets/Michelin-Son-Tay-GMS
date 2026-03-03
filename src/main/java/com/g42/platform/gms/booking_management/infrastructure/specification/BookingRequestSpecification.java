package com.g42.platform.gms.booking_management.infrastructure.specification;

import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
    public static Specification<BookingJpa> filterBooking(LocalDate date,
                                                   Boolean isGuest,
                                                          BookingEnum status) {
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
    public static Specification<BookingJpa> searchBooking(String keyword) {

        return (root, query, cb) -> {

            String like = "%" + keyword.toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.like(cb.lower(root.get("serviceCategory")), like));
            predicates.add(cb.like(cb.lower(root.get("description")), like));
            predicates.add(cb.like(root.get("status").as(String.class), like));

            Join<Object, Object> customer =
                    root.join("customer", JoinType.LEFT);

            predicates.add(cb.like(cb.lower(customer.get("fullName")), like));
            predicates.add(cb.like(cb.lower(customer.get("phone")), like));

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
    public static Specification<BookingRequestJpa> searchBookingRequest(String keyword) {

        return (root, query, cb) -> {

            String like = "%" + keyword.toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.like(cb.lower(root.get("phone")), like));
            predicates.add(cb.like(cb.lower(root.get("fullName")), like));
            predicates.add(cb.like(cb.lower(root.get("description")), like));
            predicates.add(cb.like(cb.lower(root.get("serviceCategory")), like));
            predicates.add(cb.like(cb.lower(root.get("requestCode")), like));
            predicates.add(cb.like(root.get("status").as(String.class), like));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
