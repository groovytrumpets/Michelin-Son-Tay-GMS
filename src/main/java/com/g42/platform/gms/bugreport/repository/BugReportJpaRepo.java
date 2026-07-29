package com.g42.platform.gms.bugreport.repository;

import com.g42.platform.gms.bugreport.entity.BugReportJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BugReportJpaRepo extends JpaRepository<BugReportJpa, Long>, JpaSpecificationExecutor<BugReportJpa> {
}
