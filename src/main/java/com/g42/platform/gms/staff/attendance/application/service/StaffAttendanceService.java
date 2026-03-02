package com.g42.platform.gms.staff.attendance.application.service;

import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedDetailResponse;
import com.g42.platform.gms.staff.attendance.api.dto.StaffAttendanceRes;
import com.g42.platform.gms.staff.attendance.api.mapper.StaffAttendanceDtoMapper;
import com.g42.platform.gms.staff.attendance.domain.entity.StaffAttendance;
import com.g42.platform.gms.staff.attendance.domain.repository.StaffAttendanceRepo;
import com.g42.platform.gms.staff.attendance.infrastructure.mapper.StaffAttendanceJpaMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class StaffAttendanceService {
    private StaffAttendanceRepo staffAttendanceRepo;
    private StaffAttendanceDtoMapper staffAttendanceDtoMapper;
    public List<StaffAttendanceRes> getAllAttendanceByStaffId(Integer staffId) {
        return staffAttendanceRepo.getAllStaffAttendance(staffId).stream().map(staffAttendanceDtoMapper::toDto).toList();
    }
}
