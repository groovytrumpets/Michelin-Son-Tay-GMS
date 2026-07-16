package com.g42.platform.gms.dashboard.infrastructure.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.StaffDashboardConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffDashboardConfigJpaRepo extends JpaRepository<StaffDashboardConfigJpa, Integer> {
    List<StaffDashboardConfigJpa> findAllByStaffId(Integer staffId);
    Optional<StaffDashboardConfigJpa> findByStaffIdAndIsActiveTrue(Integer staffId);
}
