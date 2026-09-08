package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.customer.infrastructure.entity.CustomerPointsJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerPointsJpaRepo extends JpaRepository<CustomerPointsJpa, Integer> {

    Optional<CustomerPointsJpa> findByCustomerId(Integer customerId);

    /** Nạp điểm/hạng của cả một trang khách trong một truy vấn, tránh N+1. */
    List<CustomerPointsJpa> findByCustomerIdIn(Collection<Integer> customerIds);

    /** Tất cả khách chưa hoạt động trong năm hiện tại (để reset điểm) */
    @Query("""
        SELECT p FROM CustomerPointsJpa p
        WHERE p.pointsResetYear < :currentYear
        AND p.lastActivityAt < :cutoffDate
    """)
    List<CustomerPointsJpa> findInactiveForReset(
            @Param("currentYear") int currentYear,
            @Param("cutoffDate") java.time.LocalDateTime cutoffDate
    );

    /** Đặt lại điểm hàng loạt (chỉ gọi từ scheduler) */
    @Modifying
    @Query("""
        UPDATE CustomerPointsJpa p SET
            p.totalPoints = 0,
            p.currentRank = 'BRONZE',
            p.pointsResetYear = :year
        WHERE p.pointsResetYear < :year
        AND p.lastActivityAt < :cutoffDate
    """)
    int resetInactivePoints(
            @Param("year") int year,
            @Param("cutoffDate") java.time.LocalDateTime cutoffDate
    );
}
