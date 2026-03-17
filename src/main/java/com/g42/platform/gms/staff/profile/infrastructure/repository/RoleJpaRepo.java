package com.g42.platform.gms.staff.profile.infrastructure.repository;

import com.g42.platform.gms.staff.profile.infrastructure.entity.RoleJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepo extends JpaRepository<RoleJpa,Integer> {
}
