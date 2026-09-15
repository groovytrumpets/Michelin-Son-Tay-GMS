package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerProfileRepository
        extends JpaRepository<CustomerProfile, Integer> {

    /** Tra khách theo số chính HOẶC số phụ (customer_phone, changeset 037) — xem CustomerProfileJpaRepo#findByPhone. */
    @Query(nativeQuery = true, value = """
            SELECT p.* FROM customer_profile p
            WHERE p.phone = :phone
               OR p.customer_id = (SELECT cp.customer_id FROM customer_phone cp WHERE cp.phone = :phone)
            ORDER BY (p.phone = :phone) DESC
            LIMIT 1
            """)
    Optional<CustomerProfile> findByPhone(@Param("phone") String phone);

    /** Đăng nhập bằng email — email lưu chữ thường, xem CustomerAuthService#normalizeEmail. */
    Optional<CustomerProfile> findByEmailIgnoreCase(String email);

    /** Chặn 2 khách hàng dùng chung email (email là định danh đăng nhập). */
    boolean existsByEmailIgnoreCaseAndCustomerIdNot(String email, Integer customerId);

    CustomerProfile getCustomerProfilesByCustomerId(Integer customerId);
    @Query("""
    select c from CustomerProfileJpa c 
    join ServiceTicketManagement st on st.customerId = c.customerId 
    where st.serviceTicketId = :serviceTicketId
""")
    CustomerProfileJpa getCustomerProfilesByServiceTicketId(@Param("serviceTicketId") Integer serviceTicketId);
}

