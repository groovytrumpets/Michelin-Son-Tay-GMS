package com.g42.platform.gms.manager.schedule.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.StaffScheduleJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ManagerStaffScheduleRepository extends JpaRepository<StaffScheduleJpa, Integer> {

    List<StaffScheduleJpa> findByWorkDateBetweenOrderByWorkDateAsc(LocalDate from, LocalDate to);

    List<StaffScheduleJpa> findByStaffIdAndWorkDateBetween(Integer staffId, LocalDate from, LocalDate to);

    @Query("SELECT COUNT(s) FROM StaffScheduleJpa s WHERE s.workDate = :date AND s.status != 'CANCELLED'")
    long countActiveByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM StaffScheduleJpa s WHERE s.status = 'COMPLETED'")
    long countCompleted();

    @Query("SELECT COUNT(s) FROM StaffScheduleJpa s WHERE s.status != 'CANCELLED'")
    long countTotalScheduled();
}
