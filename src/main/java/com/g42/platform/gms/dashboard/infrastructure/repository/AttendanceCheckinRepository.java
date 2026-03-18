package com.g42.platform.gms.dashboard.infrastructure.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.AttendanceCheckinJpa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceCheckinRepository extends JpaRepository<AttendanceCheckinJpa, Integer> {

    List<AttendanceCheckinJpa> findByStaffIdOrderByAttendanceDateDesc(Integer staffId, Pageable pageable);

    @Query("SELECT a FROM AttendanceCheckinJpa a WHERE a.staffId = :staffId " +
           "AND YEAR(a.attendanceDate) = :year AND MONTH(a.attendanceDate) = :month " +
           "ORDER BY a.attendanceDate DESC")
    List<AttendanceCheckinJpa> findByStaffIdAndMonth(@Param("staffId") Integer staffId,
                                                      @Param("year") int year,
                                                      @Param("month") int month);
}
