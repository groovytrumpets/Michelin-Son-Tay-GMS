package com.g42.platform.gms.dashboard.infrastructure.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.KpiMonthlyResultJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KpiMonthlyResultRepository extends JpaRepository<KpiMonthlyResultJpa, Integer> {
    Optional<KpiMonthlyResultJpa> findByStaffIdAndPeriodMonth(Integer staffId, String periodMonth);
    List<KpiMonthlyResultJpa> findAllByPeriodMonth(String periodMonth);
    List<KpiMonthlyResultJpa> findAllByStaffIdOrderByPeriodMonthAsc(Integer staffId);
}
