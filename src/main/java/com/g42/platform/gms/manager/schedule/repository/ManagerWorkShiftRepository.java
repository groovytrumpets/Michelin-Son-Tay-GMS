package com.g42.platform.gms.manager.schedule.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.WorkShiftJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerWorkShiftRepository extends JpaRepository<WorkShiftJpa, Integer> {
    List<WorkShiftJpa> findByIsActiveTrue();
}
