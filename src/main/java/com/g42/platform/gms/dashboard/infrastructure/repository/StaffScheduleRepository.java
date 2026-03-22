package com.g42.platform.gms.dashboard.infrastructure.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.StaffScheduleJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffScheduleRepository extends JpaRepository<StaffScheduleJpa, Integer> {

    Optional<StaffScheduleJpa> findByStaffIdAndWorkDate(Integer staffId, LocalDate workDate);

    List<StaffScheduleJpa> findByStaffIdAndWorkDateBetweenOrderByWorkDateAsc(
            Integer staffId, LocalDate from, LocalDate to);

    @Query(value = "SELECT COALESCE(SUM(TIMESTAMPDIFF(HOUR, ws.start_time, ws.end_time)), 0) " +
           "FROM staff_schedule ss " +
           "JOIN work_shift ws ON ss.shift_id = ws.shift_id " +
           "WHERE ss.staff_id = :staffId " +
           "AND YEAR(ss.work_date) = :year AND MONTH(ss.work_date) = :month " +
           "AND ss.status != 'CANCELLED'", nativeQuery = true)
    Double sumMonthlyHours(@Param("staffId") Integer staffId,
                           @Param("year") int year,
                           @Param("month") int month);
}
