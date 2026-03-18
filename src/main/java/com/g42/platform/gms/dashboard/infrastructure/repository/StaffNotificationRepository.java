package com.g42.platform.gms.dashboard.infrastructure.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.StaffNotificationJpa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffNotificationRepository extends JpaRepository<StaffNotificationJpa, Integer> {

    @Query("SELECT n FROM StaffNotificationJpa n " +
           "WHERE (n.staffId = :staffId OR n.staffId IS NULL) AND n.isRead = false " +
           "ORDER BY n.sentAt DESC")
    List<StaffNotificationJpa> findUnreadByStaffId(@Param("staffId") Integer staffId, Pageable pageable);

    @Query("SELECT n FROM StaffNotificationJpa n " +
           "WHERE (n.staffId = :staffId OR n.staffId IS NULL) " +
           "ORDER BY n.sentAt DESC")
    List<StaffNotificationJpa> findAllByStaffId(@Param("staffId") Integer staffId, Pageable pageable);
}
