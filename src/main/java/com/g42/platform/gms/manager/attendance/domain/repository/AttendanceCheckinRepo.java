package com.g42.platform.gms.manager.attendance.domain.repository;

import com.g42.platform.gms.manager.attendance.domain.entity.AttendanceCheckin;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceCheckinRepo {
    List<AttendanceCheckin> findByDateRange(LocalDate from, LocalDate to);
    List<AttendanceCheckin> findByStaffAndDateRange(Integer staffId, LocalDate from, LocalDate to);
    List<AttendanceCheckin> findByDate(LocalDate date);
    Optional<AttendanceCheckin> findById(Integer checkinId);
    Optional<AttendanceCheckin> findByStaffAndDateAndShift(Integer staffId, LocalDate date, Integer shiftId);
    AttendanceCheckin save(AttendanceCheckin checkin);
    void deleteById(Integer checkinId);
}
