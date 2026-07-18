package com.g42.platform.gms.attendancerequest.infrastructure.repository;

import com.g42.platform.gms.attendancerequest.infrastructure.entity.AttendanceRequestJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRequestJpaRepo extends JpaRepository<AttendanceRequestJpa, Integer> {
    List<AttendanceRequestJpa> findByStaffIdOrderByCreatedAtDesc(Integer staffId);
    List<AttendanceRequestJpa> findByStatusOrderByCreatedAtDesc(String status);
    List<AttendanceRequestJpa> findByRequestTypeOrderByCreatedAtDesc(String requestType);
    List<AttendanceRequestJpa> findByStatusAndRequestTypeOrderByCreatedAtDesc(String status, String requestType);
    List<AttendanceRequestJpa> findAllByOrderByCreatedAtDesc();
}
