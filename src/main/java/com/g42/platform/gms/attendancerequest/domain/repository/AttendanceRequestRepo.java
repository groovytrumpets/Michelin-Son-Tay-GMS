package com.g42.platform.gms.attendancerequest.domain.repository;

import com.g42.platform.gms.attendancerequest.domain.entity.AttendanceRequest;

import java.util.List;
import java.util.Optional;

public interface AttendanceRequestRepo {
    List<AttendanceRequest> findByStaffId(Integer staffId);
    List<AttendanceRequest> findByFilters(String status, String requestType);
    Optional<AttendanceRequest> findById(Integer requestId);
    AttendanceRequest save(AttendanceRequest request);
    void deleteById(Integer requestId);
}
