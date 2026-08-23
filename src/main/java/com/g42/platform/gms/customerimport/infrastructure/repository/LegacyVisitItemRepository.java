package com.g42.platform.gms.customerimport.infrastructure.repository;

import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LegacyVisitItemRepository extends JpaRepository<LegacyVisitItemJpa, Integer> {

    List<LegacyVisitItemJpa> findByLegacyVisitIdOrderByLineNoAsc(Integer legacyVisitId);

    List<LegacyVisitItemJpa> findByLegacyVisitIdInOrderByLineNoAsc(Collection<Integer> legacyVisitIds);
}
