package com.g42.platform.gms.manager.attendancelocation.infrastructure.repository;

import com.g42.platform.gms.manager.attendancelocation.infrastructure.entity.AttendanceLocationJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceLocationJpaRepo extends JpaRepository<AttendanceLocationJpa, Integer> {
    Optional<AttendanceLocationJpa> findByQrToken(String qrToken);
    boolean existsByQrToken(String qrToken);
}
