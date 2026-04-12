package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.infrastructure.entity.ServiceReminderJpa;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;

public class ServiceReminderSpecification {
    public static Specification<ServiceReminderJpa> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) return null;
            return cb.equal(root.get("status"), status);
        };
    }
    public static Specification<ServiceReminderJpa> hasDate(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return null;
            return cb.equal(root.get("reminderDate"), date);
        };
    }
    public static Specification<ServiceReminderJpa> hasTime(LocalTime time) {
        return (root, query, cb) -> {
            if (time == null) return null;
            return cb.equal(root.get("reminderTime"), time);
        };
    }
    public static Specification<ServiceReminderJpa> containsText(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;

            String searchPattern = "%" + search.toLowerCase() + "%";

            Join<Object, Object> customerJoin = root.join("customerProfile", JoinType.LEFT);
            Join<Object, Object> vehicleJoin = root.join("vehicle", JoinType.LEFT);
            Join<Object, Object> ticketJoin = root.join("serviceTicket", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(customerJoin.get("fullName")), searchPattern),
                    cb.like(cb.lower(customerJoin.get("phone")), searchPattern),
                    cb.like(cb.lower(vehicleJoin.get("licensePlate")), searchPattern),
                    cb.like(cb.lower(ticketJoin.get("ticketCode")), searchPattern)
            );
        };
    }
}
