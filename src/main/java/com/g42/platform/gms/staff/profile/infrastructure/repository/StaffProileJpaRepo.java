package com.g42.platform.gms.staff.profile.infrastructure.repository;

import com.g42.platform.gms.staff.profile.domain.entity.StaffAuth;
import com.g42.platform.gms.staff.profile.domain.entity.StaffProfile;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffProileJpaRepo extends JpaRepository<StaffProfileJpa, Integer> {
    @Query("""
        SELECT DISTINCT sp FROM StaffProfileJpa sp
        LEFT JOIN FETCH sp.staffAuth sa
        LEFT JOIN FETCH sp.roles r
        WHERE (:search IS NULL OR sp.fullName LIKE %:search% OR sp.phone LIKE %:search%)
        AND (:status IS NULL OR sa.status = :status)
        AND (:roleIds IS NULL OR r.id IN :roleIds)
    """)
    Page<StaffProfileJpa> findAllWithFilter(
            @Param("search") String search,
            @Param("status") String status,
            @Param("roleIds") List<Integer> roleIds,
            Pageable pageable
    );

    StaffProfileJpa findByStaffId(Integer staffId);

    boolean existsByPhone(String phone);
}
