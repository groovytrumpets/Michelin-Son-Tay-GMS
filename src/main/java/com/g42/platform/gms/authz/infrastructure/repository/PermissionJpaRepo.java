package com.g42.platform.gms.authz.infrastructure.repository;

import com.g42.platform.gms.authz.infrastructure.entity.PermissionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionJpaRepo extends JpaRepository<PermissionJpa, String> {

    List<PermissionJpa> findAllByOrderBySortOrderAsc();
}
