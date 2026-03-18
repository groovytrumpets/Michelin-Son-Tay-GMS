package com.g42.platform.gms.staff.attendance.infrastructure.repository;

import com.g42.platform.gms.staff.attendance.infrastructure.entity.StaffAttendanceJpa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StaffAttendanceJpaRepo extends JpaRepository<StaffAttendanceJpa, Integer> {
    List<StaffAttendanceJpa> getStaffAttendanceJpaByStaff_StaffId(Integer staffStaffId);

    @Query("SELECT a FROM StaffAttendanceJpa a WHERE a.staff.staffId = :staffId AND YEAR(a.attendanceDate) = :year AND MONTH(a.attendanceDate) = :month")
    List<StaffAttendanceJpa> findByStaffIdAndMonth(@Param("staffId") Integer staffId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT a FROM StaffAttendanceJpa a WHERE a.staff.staffId = :staffId ORDER BY a.attendanceDate DESC")
    List<StaffAttendanceJpa> findRecentByStaffId(@Param("staffId") Integer staffId, Pageable pageable);
}
