package com.g42.platform.gms.manager.attendance.infrastructure.implement;

import com.g42.platform.gms.manager.attendance.domain.entity.AttendanceCheckin;
import com.g42.platform.gms.manager.attendance.domain.repository.AttendanceCheckinRepo;
import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import com.g42.platform.gms.manager.attendance.infrastructure.mapper.AttendanceCheckinJpaMapper;
import com.g42.platform.gms.manager.attendance.infrastructure.repository.AttendanceCheckinJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AttendanceCheckinRepoImpl implements AttendanceCheckinRepo {

    private final AttendanceCheckinJpaRepo jpaRepo;
    private final AttendanceCheckinJpaMapper mapper;

    @Override
    public List<AttendanceCheckin> findByDateRange(LocalDate from, LocalDate to) {
        return jpaRepo.findByDateRange(from, to).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AttendanceCheckin> findByStaffAndDateRange(Integer staffId, LocalDate from, LocalDate to) {
        return jpaRepo.findByStaffAndDateRange(staffId, from, to).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AttendanceCheckin> findByDate(LocalDate date) {
        return jpaRepo.findByAttendanceDate(date).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<AttendanceCheckin> findById(Integer checkinId) {
        return jpaRepo.findById(checkinId).map(mapper::toDomain);
    }

    @Override
    public Optional<AttendanceCheckin> findByStaffAndDateAndShift(Integer staffId, LocalDate date, Integer shiftId) {
        return jpaRepo.findByStaffIdAndAttendanceDateAndShiftId(staffId, date, shiftId).map(mapper::toDomain);
    }

    @Override
    public AttendanceCheckin save(AttendanceCheckin checkin) {
        AttendanceCheckinJpa saved = jpaRepo.save(mapper.toJpa(checkin));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(Integer checkinId) {
        jpaRepo.deleteById(checkinId);
    }
}
