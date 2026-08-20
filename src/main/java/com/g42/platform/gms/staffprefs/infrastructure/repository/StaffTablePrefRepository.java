package com.g42.platform.gms.staffprefs.infrastructure.repository;

import com.g42.platform.gms.staffprefs.infrastructure.entity.StaffTablePrefJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffTablePrefRepository extends JpaRepository<StaffTablePrefJpa, Integer> {

    Optional<StaffTablePrefJpa> findByStaffIdAndTableKey(Integer staffId, String tableKey);
}
