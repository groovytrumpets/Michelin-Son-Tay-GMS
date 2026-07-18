package com.g42.platform.gms.systemlog.repository;

import com.g42.platform.gms.systemlog.entity.SystemLogJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemLogJpaRepo extends JpaRepository<SystemLogJpa, Long>, JpaSpecificationExecutor<SystemLogJpa> {
}
