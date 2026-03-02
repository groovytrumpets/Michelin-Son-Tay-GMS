package com.g42.platform.gms.staff.attendance.api.dto;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.staff.attendance.domain.enums.AttendanceSlotEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffAttendanceRes {
    private LocalDate attendanceDate;
    private AttendanceSlotEnum morningStatus;
    private AttendanceSlotEnum afternoonStatus;
}
