package com.g42.platform.gms.customerimport.infrastructure.repository;

import com.g42.platform.gms.customerimport.infrastructure.entity.ImportBatchJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatchJpa, Integer> {

    List<ImportBatchJpa> findAllByOrderByImportedAtDesc();
}
