package com.g42.platform.gms.staff.profile.infrastructure.repository;

import com.g42.platform.gms.staff.profile.domain.entity.StaffAuth;
import com.g42.platform.gms.staff.profile.domain.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffProileJpaRepo extends JpaRepository<StaffProfile, Integer> {
}
