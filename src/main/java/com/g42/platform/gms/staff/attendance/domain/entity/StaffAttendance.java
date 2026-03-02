package com.g42.platform.gms.staff.attendance.domain.entity;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.staff.attendance.domain.enums.AttendanceSlotEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
public class StaffAttendance {

    private Long id;
    private StaffProfile staff;
    private LocalDate attendanceDate;
    private AttendanceSlotEnum morningStatus;
    private AttendanceSlotEnum afternoonStatus;
    private Instant createdAt;
    private Instant updatedAt;


}