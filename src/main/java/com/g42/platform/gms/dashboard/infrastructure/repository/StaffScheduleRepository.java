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

    @Query("SELECT COALESCE(SUM(TIMESTAMPDIFF(HOUR, s.shift.startTime, s.shift.endTime)), 0) " +
           "FROM StaffScheduleJpa s " +
           "WHERE s.staffId = :staffId " +
           "AND YEAR(s.workDate) = :year AND MONTH(s.workDate) = :month " +
           "AND s.status != 'CANCELLED'")
    Double sumMonthlyHours(@Param("staffId") Integer staffId,
                           @Param("year") int year,
                           @Param("month") int month);
}
