package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.StaffAuth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAuthRepo extends JpaRepository<StaffAuth, Integer> {
    StaffAuth searchByEmail(String email);

    StaffAuth searchByStaffAuthId(long staffAuthId);

    StaffAuth getStaffAuthByEmail(String email);
}
