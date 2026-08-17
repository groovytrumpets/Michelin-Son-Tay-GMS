package com.g42.platform.gms.marketing.recruitment.infrastructure.repository;

import com.g42.platform.gms.marketing.recruitment.domain.ApplicationStatus;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobApplicationJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface JobApplicationJpaRepo extends JpaRepository<JobApplicationJpa, Long>,
        JpaSpecificationExecutor<JobApplicationJpa> {

    boolean existsByCode(String code);

    long countByStatus(ApplicationStatus status);

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    /**
     * Chặn bấm gửi nhiều lần: cùng số điện thoại nộp lại cùng một vị trí trong
     * khoảng thời gian ngắn thì coi là trùng.
     *
     * <p>Chặn theo số điện thoại chứ không theo IP vì cả xưởng có thể dùng
     * chung một đường mạng, mà chặn theo IP thì người thứ hai không nộp được.
     */
    boolean existsByJob_JobIdAndPhoneAndCreatedAtGreaterThanEqual(Long jobId, String phone, LocalDateTime from);

    /** Số hồ sơ đã nhận trong tháng, dùng sinh phần số của mã hồ sơ. */
    @Query("SELECT COUNT(a) FROM JobApplicationJpa a WHERE a.createdAt >= :from AND a.createdAt < :to")
    long countInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
