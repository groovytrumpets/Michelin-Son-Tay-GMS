package com.g42.platform.gms.catalogdraft.infrastructure.repository;

import com.g42.platform.gms.catalogdraft.infrastructure.entity.CatalogDraftJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogDraftRepository extends JpaRepository<CatalogDraftJpa, Long> {
    List<CatalogDraftJpa> findByStaffIdAndDraftTypeOrderByUpdatedAtDesc(Integer staffId, String draftType);
    Optional<CatalogDraftJpa> findByDraftIdAndStaffId(Long draftId, Integer staffId);
}
