package com.g42.platform.gms.manager.attendance.infrastructure.repository;

import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceCheckinJpaRepo extends JpaRepository<AttendanceCheckinJpa, Integer> {

    @Query("SELECT a FROM ManagerAttendanceCheckinJpa a WHERE a.attendanceDate BETWEEN :from AND :to ORDER BY a.attendanceDate DESC, a.staffId")
    List<AttendanceCheckinJpa> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT a FROM ManagerAttendanceCheckinJpa a WHERE a.staffId = :staffId AND a.attendanceDate BETWEEN :from AND :to ORDER BY a.attendanceDate DESC")
    List<AttendanceCheckinJpa> findByStaffAndDateRange(@Param("staffId") Integer staffId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<AttendanceCheckinJpa> findByAttendanceDate(LocalDate date);

    Optional<AttendanceCheckinJpa> findByStaffIdAndAttendanceDateAndShiftId(Integer staffId, LocalDate date, Integer shiftId);

    List<AttendanceCheckinJpa> findByStaffIdAndAttendanceDate(Integer staffId, LocalDate date);

    List<AttendanceCheckinJpa> findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(Integer staffId, LocalDate from, LocalDate to);

    @Query("SELECT COUNT(DISTINCT a.attendanceDate) FROM ManagerAttendanceCheckinJpa a WHERE a.staffId = :staffId AND a.attendanceDate BETWEEN :from AND :to")
    int countWorkDays(@Param("staffId") Integer staffId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT COALESCE(SUM(TIMESTAMPDIFF(HOUR, ws.start_time, ws.end_time)), 0) " +
           "FROM attendance_checkin ac " +
           "JOIN work_shift ws ON ac.shift_id = ws.shift_id " +
           "WHERE ac.staff_id = :staffId " +
           "AND YEAR(ac.attendance_date) = :year AND MONTH(ac.attendance_date) = :month",
           nativeQuery = true)
    Double sumMonthlyHours(@Param("staffId") Integer staffId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT MAX(a.createdAt) FROM ManagerAttendanceCheckinJpa a WHERE a.notes = :notes")
    Optional<LocalDateTime> findMaxCreatedAtByNotes(@Param("notes") String notes);
}
