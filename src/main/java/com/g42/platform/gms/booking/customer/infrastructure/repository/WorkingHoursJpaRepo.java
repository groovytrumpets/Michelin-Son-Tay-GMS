package com.g42.platform.gms.booking.customer.infrastructure.repository;

import com.g42.platform.gms.booking.customer.infrastructure.entity.WorkingHoursJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkingHoursJpaRepo extends JpaRepository<WorkingHoursJpa, Integer> {
    Optional<WorkingHoursJpa> findByDayOfWeek(Integer dayOfWeek);

    List<WorkingHoursJpa> findAllByOrderByDayOfWeekAsc();
}
