package com.g42.platform.gms.staff.attendance.infrastructure.repository;

import com.g42.platform.gms.staff.attendance.infrastructure.entity.StaffAttendanceJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAttendanceJpaRepo extends JpaRepository<StaffAttendanceJpa, Integer> {
}
