package com.g42.platform.gms.manager.schedule.infrastructure.repository;

import com.g42.platform.gms.manager.schedule.infrastructure.entity.WorkShiftJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkShiftJpaRepo extends JpaRepository<WorkShiftJpa, Integer> {
}
