package com.g42.platform.gms.manager.attendance.infrastructure.repository;

import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceCheckinJpaRepo extends JpaRepository<AttendanceCheckinJpa, Integer> {

    @Query("SELECT a FROM AttendanceCheckinJpa a WHERE a.attendanceDate BETWEEN :from AND :to ORDER BY a.attendanceDate DESC, a.staffId")
    List<AttendanceCheckinJpa> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT a FROM AttendanceCheckinJpa a WHERE a.staffId = :staffId AND a.attendanceDate BETWEEN :from AND :to ORDER BY a.attendanceDate DESC")
    List<AttendanceCheckinJpa> findByStaffAndDateRange(@Param("staffId") Integer staffId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<AttendanceCheckinJpa> findByAttendanceDate(LocalDate date);

    Optional<AttendanceCheckinJpa> findByStaffIdAndAttendanceDateAndShiftId(Integer staffId, LocalDate date, Integer shiftId);

    @Query("SELECT COUNT(DISTINCT a.attendanceDate) FROM AttendanceCheckinJpa a WHERE a.staffId = :staffId AND a.attendanceDate BETWEEN :from AND :to")
    int countWorkDays(@Param("staffId") Integer staffId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
