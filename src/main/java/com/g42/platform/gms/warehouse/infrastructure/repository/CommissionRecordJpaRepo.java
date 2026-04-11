package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.CommissionRecordJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommissionRecordJpaRepo extends JpaRepository<CommissionRecordJpa, Integer> {

    List<CommissionRecordJpa> findByStaffIdAndPeriodMonth(Integer staffId, String periodMonth);

    List<CommissionRecordJpa> findByPeriodMonth(String periodMonth);

    @Query("SELECT COALESCE(SUM(c.quantity), 0) FROM CommissionRecordJpa c WHERE c.staffId = :staffId AND c.itemId = :itemId AND c.periodMonth = :periodMonth")
    Integer sumQuantityByStaffAndItemAndPeriod(
            @Param("staffId") Integer staffId,
            @Param("itemId") Integer itemId,
            @Param("periodMonth") String periodMonth);
}
