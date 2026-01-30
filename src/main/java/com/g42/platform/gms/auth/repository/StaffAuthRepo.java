package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.Staffauth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAuthRepo extends JpaRepository<Staffauth, Integer> {
    Object searchByEmail(String email);
}
