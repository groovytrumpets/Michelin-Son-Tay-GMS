package com.g42.platform.gms.staff.attendance.domain.repository;

import com.g42.platform.gms.staff.attendance.domain.entity.StaffAttendance;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffAttendanceRepo {
    List<StaffAttendance> getAllStaffAttendance(Integer staffId);
}
