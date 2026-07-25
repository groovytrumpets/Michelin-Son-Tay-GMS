package com.g42.platform.gms.docs.infrastructure.repository;

import com.g42.platform.gms.docs.infrastructure.entity.StaffDocsProgressJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffDocsProgressRepository extends JpaRepository<StaffDocsProgressJpa, Long> {

    List<StaffDocsProgressJpa> findByStaffId(Integer staffId);

    Optional<StaffDocsProgressJpa> findByStaffIdAndTopicId(Integer staffId, String topicId);

    @Query("SELECT COUNT(p) FROM StaffDocsProgressJpa p WHERE p.staffId = :staffId AND p.status = 'COMPLETED'")
    long countCompletedByStaffId(Integer staffId);

    @Query("SELECT p.staffId, COUNT(p) FROM StaffDocsProgressJpa p WHERE p.status = 'COMPLETED' GROUP BY p.staffId")
    List<Object[]> countCompletedGroupByStaffId();
}
