package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.Role;
import com.g42.platform.gms.auth.entity.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffRoleRepository extends JpaRepository<StaffRole,Integer> {
    List<StaffRole> getStaffRoleByStaff_StaffId(Integer staffStaffId);
}
