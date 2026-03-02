package com.g42.platform.gms.staff.attendance.infrastructure.repository;

import com.g42.platform.gms.staff.attendance.infrastructure.entity.StaffAttendanceJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffAttendanceJpaRepo extends JpaRepository<StaffAttendanceJpa, Integer> {
    List<StaffAttendanceJpa> getStaffAttendanceJpaByStaff_StaffId(Integer staffStaffId);
}
