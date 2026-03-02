package com.g42.platform.gms.staff.attendance.infrastructure.entity;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.staff.attendance.domain.enums.AttendanceSlotEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "staff_attendance", schema = "michelin_garage")
public class StaffAttendanceJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idstaff_attendance", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffProfile staff;

    @Column(name = "attendance_date")
    private LocalDate attendanceDate;

    @Lob
    @Column(name = "morning_status")
    private AttendanceSlotEnum morningStatus;

    @Lob
    @Column(name = "afternoon_status")
    private AttendanceSlotEnum afternoonStatus;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;


}