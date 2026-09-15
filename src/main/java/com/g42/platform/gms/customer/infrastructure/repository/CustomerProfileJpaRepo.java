package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerProfileJpaRepo extends JpaRepository<CustomerProfileJpa,Integer> , JpaSpecificationExecutor<CustomerProfileJpa> {

    CustomerProfileJpa findByCustomerId(Integer customerId);
    
    /**
     * Tra khách theo số chính HOẶC số phụ (customer_phone, changeset 037). Mọi chỗ kiểm tra
     * "số này đã có khách chưa" đi qua đây nên số phụ cũng được tính là đã có chủ.
     */
    @Query(nativeQuery = true, value = """
            SELECT p.* FROM customer_profile p
            WHERE p.phone = :phone
               OR p.customer_id = (SELECT cp.customer_id FROM customer_phone cp WHERE cp.phone = :phone)
            ORDER BY (p.phone = :phone) DESC
            LIMIT 1
            """)
    CustomerProfileJpa findByPhone(@Param("phone") String phone);

    java.util.Optional<CustomerProfileJpa> findByCustomerCodeIgnoreCase(String customerCode);

    /** Email là định danh đăng nhập thứ hai của khách hàng nên phải kiểm tra trùng trước khi lưu. */
    java.util.Optional<CustomerProfileJpa> findByEmailIgnoreCase(String email);
}
