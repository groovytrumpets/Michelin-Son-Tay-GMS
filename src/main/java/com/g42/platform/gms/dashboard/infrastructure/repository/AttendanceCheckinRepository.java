package com.g42.platform.gms.dashboard.infrastructure.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.AttendanceCheckinJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceCheckinRepository extends JpaRepository<AttendanceCheckinJpa, Integer> {

    List<AttendanceCheckinJpa> findByStaffIdAndAttendanceDate(Integer staffId, LocalDate date);

    List<AttendanceCheckinJpa> findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Integer staffId, LocalDate from, LocalDate to);

    @Query(value = "SELECT COALESCE(SUM(TIMESTAMPDIFF(HOUR, ws.start_time, ws.end_time)), 0) " +
           "FROM attendance_checkin ac " +
           "JOIN work_shift ws ON ac.shift_id = ws.shift_id " +
           "WHERE ac.staff_id = :staffId " +
           "AND YEAR(ac.attendance_date) = :year AND MONTH(ac.attendance_date) = :month",
           nativeQuery = true)
    Double sumMonthlyHours(@Param("staffId") Integer staffId,
                           @Param("year") int year,
                           @Param("month") int month);
}
