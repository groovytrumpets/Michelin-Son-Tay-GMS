package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.customer.infrastructure.entity.CustomerPointsHistoryJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CustomerPointsHistoryJpaRepo extends JpaRepository<CustomerPointsHistoryJpa, Integer> {
    Page<CustomerPointsHistoryJpa> findByCustomerIdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);
    long countByCustomerIdAndReason(Integer customerId, String reason);

    /**
     * Đếm số lượt theo lý do cho nhiều khách trong một truy vấn GROUP BY, tránh N+1
     * khi dựng danh sách khách hàng. Mỗi phần tử: [customerId (Integer), count (Long)].
     */
    @Query("""
        SELECT h.customerId, COUNT(h) FROM CustomerPointsHistoryJpa h
        WHERE h.reason = :reason AND h.customerId IN :customerIds
        GROUP BY h.customerId
    """)
    List<Object[]> countByReasonGroupedByCustomer(@Param("reason") String reason,
                                                  @Param("customerIds") Collection<Integer> customerIds);
}
