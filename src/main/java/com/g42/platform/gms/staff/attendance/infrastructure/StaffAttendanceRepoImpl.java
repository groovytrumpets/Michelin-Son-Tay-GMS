package com.g42.platform.gms.staff.attendance.infrastructure;

import com.g42.platform.gms.staff.attendance.domain.entity.StaffAttendance;
import com.g42.platform.gms.staff.attendance.domain.repository.StaffAttendanceRepo;
import com.g42.platform.gms.staff.attendance.infrastructure.entity.StaffAttendanceJpa;
import com.g42.platform.gms.staff.attendance.infrastructure.mapper.StaffAttendanceJpaMapper;
import com.g42.platform.gms.staff.attendance.infrastructure.repository.StaffAttendanceJpaRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
@AllArgsConstructor
public class StaffAttendanceRepoImpl implements StaffAttendanceRepo {
    private final StaffAttendanceJpaRepo staffAttendanceJpaRepo;
    private final StaffAttendanceJpaMapper staffAttendanceJpaMapper;
    @Override
    public List<StaffAttendance> getAllStaffAttendance(Integer staffId) {
        return staffAttendanceJpaRepo.getStaffAttendanceJpaByStaff_StaffId(staffId).stream().map(staffAttendanceJpaMapper::toDomain).toList();
    }
}
