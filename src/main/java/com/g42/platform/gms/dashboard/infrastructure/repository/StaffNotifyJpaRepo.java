package com.g42.platform.gms.dashboard.infrastructure.repository;

import com.g42.platform.gms.dashboard.infrastructure.entity.StaffNotificationJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffNotifyJpaRepo extends JpaRepository<StaffNotificationJpa, Integer>{

    List<StaffNotificationJpa> findAllByStaffId(Integer staffId);
    @Query("""
        select n from StaffNotificationJpa n where (n.staffId=:staffId or n.staffId is null ) order by n.isRead asc,
                 n.sentAt desc
        """)
    List<StaffNotificationJpa> findAllByStaffIdOrStaffIdIsNull(@Param("staffId") Integer staffId);
}
