package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffProfileRepo extends JpaRepository<StaffProfile,Integer> {

    StaffProfile searchByPhone(String phone);
}
