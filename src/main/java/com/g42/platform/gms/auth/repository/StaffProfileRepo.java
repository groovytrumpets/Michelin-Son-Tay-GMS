package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.Role;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.entity.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffProfileRepo extends JpaRepository<StaffProfile,Integer> {

    StaffProfile searchByPhone(String phone);

    StaffProfile getStaffProfileByStaffauth_StaffAuthId(Integer staffAuthId);

    @Query("SELECT sp FROM StaffProfile sp JOIN sp.staffRoles sr JOIN sr.role r WHERE r.roleCode = :roleCode")
    List<StaffProfile> findByRoleCode(@Param("roleCode") String roleCode);

    Optional<StaffProfile> findByEmployeeNo(String employeeNo);

}
