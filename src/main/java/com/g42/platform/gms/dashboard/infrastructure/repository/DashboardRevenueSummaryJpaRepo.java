package com.g42.platform.gms.dashboard.infrastructure.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.DashboardRevenueSummaryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardRevenueSummaryJpaRepo extends JpaRepository<DashboardRevenueSummaryJpa, Long> {
    
    Optional<DashboardRevenueSummaryJpa> findByReportDate(LocalDate reportDate);

    List<DashboardRevenueSummaryJpa> findByReportDateBetweenOrderByReportDateAsc(LocalDate fromDate, LocalDate toDate);
}
