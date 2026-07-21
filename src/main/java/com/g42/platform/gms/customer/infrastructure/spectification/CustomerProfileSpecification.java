package com.g42.platform.gms.customer.infrastructure.spectification;

import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerAuthJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CustomerProfileSpecification {
    public static Specification<CustomerProfileJpa> filter(LocalDate date, String status) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (date != null) {
                predicates.add(
                        cb.equal(root.get("dob"), date)
                );
            }

            if (status != null) {
                Subquery<Integer> subquery = query.subquery(Integer.class);
                Root<CustomerAuthJpa> authRoot = subquery.from(CustomerAuthJpa.class);
                subquery.select(authRoot.get("customerId"))
                        .where(cb.equal(authRoot.get("status"), CustomerStatus.valueOf(status)));
                predicates.add(root.get("customerId").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    public static Specification<CustomerProfileJpa> searchProfiles(String keyword) {

        return (root, query, cb) -> {

            String like = "%" + keyword.toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.like(root.get("customerId").as(String.class), like));
            predicates.add(cb.like(cb.lower(root.get("fullName")), like));
            predicates.add(cb.like(root.get("phone").as(String.class), like));
            predicates.add(cb.like(root.get("email").as(String.class), like));
            predicates.add(cb.like(root.get("dob").as(String.class), like));

            Subquery<Integer> vehicleSubquery = query.subquery(Integer.class);
            Root<Vehicle> vehicleRoot = vehicleSubquery.from(Vehicle.class);
            vehicleSubquery.select(vehicleRoot.get("customer").get("customerId"))
                    .where(cb.like(cb.lower(vehicleRoot.get("licensePlate")), like));
            predicates.add(root.get("customerId").in(vehicleSubquery));

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
