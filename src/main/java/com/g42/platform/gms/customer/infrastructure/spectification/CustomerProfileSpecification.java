package com.g42.platform.gms.customer.infrastructure.spectification;

import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerAuthJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerPhoneJpa;
import com.g42.platform.gms.vehicle.support.PlateKeys;
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
            } else {
                // "Tất cả" (không lọc trạng thái) mặc định KHÔNG hiện khách đã xóa mềm — muốn xem
                // lại thì chọn riêng bộ lọc "Đã xóa". Áp dụng cho cả danh sách lẫn tìm kiếm vì
                // specification này luôn được AND với searchProfiles().
                Subquery<Integer> deletedSubquery = query.subquery(Integer.class);
                Root<CustomerAuthJpa> deletedAuthRoot = deletedSubquery.from(CustomerAuthJpa.class);
                deletedSubquery.select(deletedAuthRoot.get("customerId"))
                        .where(cb.equal(deletedAuthRoot.get("status"), CustomerStatus.DELETED));
                predicates.add(cb.not(root.get("customerId").in(deletedSubquery)));
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
            // Danh bạ đối tác: tìm theo mã khách hàng, mã số thuế, người liên hệ
            predicates.add(cb.like(cb.lower(root.get("customerCode")), like));
            predicates.add(cb.like(cb.lower(root.get("taxCode")), like));
            predicates.add(cb.like(cb.lower(root.get("address")), like));
            predicates.add(cb.like(cb.lower(root.get("contactName")), like));
            predicates.add(cb.like(cb.lower(root.get("contactPhone")), like));
            predicates.add(cb.like(cb.lower(root.get("representativeName")), like));

            Subquery<Integer> vehicleSubquery = query.subquery(Integer.class);
            Root<Vehicle> vehicleRoot = vehicleSubquery.from(Vehicle.class);
            vehicleSubquery.select(vehicleRoot.get("customer").get("customerId"))
                    .where(cb.like(cb.lower(vehicleRoot.get("licensePlate")), like));
            predicates.add(root.get("customerId").in(vehicleSubquery));

            // Tìm theo cả số điện thoại phụ (một khách nhiều số, changeset 037)
            Subquery<Integer> phoneSubquery = query.subquery(Integer.class);
            Root<CustomerPhoneJpa> phoneRoot = phoneSubquery.from(CustomerPhoneJpa.class);
            phoneSubquery.select(phoneRoot.get("customerId"))
                    .where(cb.like(phoneRoot.get("phone"), like));
            predicates.add(root.get("customerId").in(phoneSubquery));

            // Biển số gõ kiểu "30K-866.94" vẫn khớp xe lưu "30K86694"
            String plateKey = PlateKeys.normalize(keyword);
            if (plateKey != null) {
                Subquery<Integer> plateKeySubquery = query.subquery(Integer.class);
                Root<Vehicle> plateKeyRoot = plateKeySubquery.from(Vehicle.class);
                plateKeySubquery.select(plateKeyRoot.get("customer").get("customerId"))
                        .where(cb.like(plateKeyRoot.get("plateKey"), "%" + plateKey + "%"));
                predicates.add(root.get("customerId").in(plateKeySubquery));
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
