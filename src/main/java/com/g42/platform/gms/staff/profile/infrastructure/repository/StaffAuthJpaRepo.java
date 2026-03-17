package com.g42.platform.gms.staff.profile.infrastructure.repository;

import com.g42.platform.gms.staff.profile.domain.entity.StaffAuth;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffAuthJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffAuthJpaRepo extends JpaRepository<StaffAuthJpa, Integer> {
    boolean existsByEmail(String email);

    boolean existsByGoogleId(String googleId);
}
