package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.Role;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.entity.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffProfileRepo extends JpaRepository<StaffProfile,Integer> {

    StaffProfile searchByPhone(String phone);

    StaffProfile getStaffProfileByStaffauth_StaffAuthId(Integer staffAuthId);

    ;
}
