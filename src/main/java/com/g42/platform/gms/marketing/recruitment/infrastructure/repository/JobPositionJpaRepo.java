package com.g42.platform.gms.marketing.recruitment.infrastructure.repository;

import com.g42.platform.gms.marketing.recruitment.domain.JobStatus;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobPositionJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobPositionJpaRepo extends JpaRepository<JobPositionJpa, Long>,
        JpaSpecificationExecutor<JobPositionJpa> {

    Optional<JobPositionJpa> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlug(String slug);

    Optional<JobPositionJpa> findByJobIdAndDeletedAtIsNull(Long jobId);

    long countByStatusAndDeletedAtIsNull(JobStatus status);

    /** Danh sách bộ phận đang có tin, dùng đổ vào ô lọc ngoài trang khách. */
    @Query("""
            SELECT DISTINCT j.department FROM JobPositionJpa j
            WHERE j.deletedAt IS NULL
              AND j.department IS NOT NULL
              AND j.department <> ''
              AND j.status IN :statuses
            ORDER BY j.department
            """)
    List<String> findDistinctDepartments(@Param("statuses") List<JobStatus> statuses);

    /**
     * Tăng bộ đếm bằng UPDATE nguyên tử. Đọc rồi ghi lại ở tầng ứng dụng sẽ mất
     * lượt khi nhiều người mở tin cùng lúc.
     */
    @Modifying
    @Query("UPDATE JobPositionJpa j SET j.viewCount = COALESCE(j.viewCount, 0) + 1 WHERE j.jobId = :jobId")
    void incrementViewCount(@Param("jobId") Long jobId);

    @Modifying
    @Query("UPDATE JobPositionJpa j SET j.applicationCount = COALESCE(j.applicationCount, 0) + 1 WHERE j.jobId = :jobId")
    void incrementApplicationCount(@Param("jobId") Long jobId);
}
