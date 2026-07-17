package com.g42.platform.gms.manager.attendancelocation.domain.repository;

import com.g42.platform.gms.manager.attendancelocation.domain.entity.AttendanceLocation;

import java.util.List;
import java.util.Optional;

public interface AttendanceLocationRepo {
    List<AttendanceLocation> findAll();
    Optional<AttendanceLocation> findById(Integer locationId);
    Optional<AttendanceLocation> findByQrToken(String qrToken);
    boolean existsByQrToken(String qrToken);
    AttendanceLocation save(AttendanceLocation location);
}
