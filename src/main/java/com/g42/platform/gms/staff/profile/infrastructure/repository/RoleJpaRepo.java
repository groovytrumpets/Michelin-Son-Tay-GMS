package com.g42.platform.gms.staff.profile.infrastructure.repository;

import com.g42.platform.gms.staff.profile.infrastructure.entity.RoleJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleJpaRepo extends JpaRepository<RoleJpa,Integer> {

    Optional<RoleJpa> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);

    List<RoleJpa> findAllByOrderByIdAsc();

    /** Số nhân viên đang giữ mỗi vai trò — dùng để chặn xoá vai trò còn người dùng. */
    @Query(value = "SELECT role_id, COUNT(*) FROM staff_role GROUP BY role_id", nativeQuery = true)
    List<Object[]> countStaffPerRole();
}
