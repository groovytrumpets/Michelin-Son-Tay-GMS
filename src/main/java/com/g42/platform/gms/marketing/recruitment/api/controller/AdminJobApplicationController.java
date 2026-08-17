package com.g42.platform.gms.marketing.recruitment.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.recruitment.api.dto.RecruitmentDtos;
import com.g42.platform.gms.marketing.recruitment.app.JobApplicationService;
import com.g42.platform.gms.marketing.recruitment.app.RecruitmentMailService;
import com.g42.platform.gms.marketing.recruitment.domain.ApplicationStatus;
import com.g42.platform.gms.marketing.recruitment.domain.NotifyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** Xử lý hồ sơ ứng viên đã nộp. */
@Slf4j
@RestController
@RequestMapping("/api/admin/recruitment/applications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'RECEPTIONIST')")
public class AdminJobApplicationController {

    private final JobApplicationService applicationService;
    private final RecruitmentMailService mailService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RecruitmentDtos.ApplicationSummaryDto>>> search(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "applicationId"));

        // "đến ngày" người dùng chọn được hiểu là hết ngày hôm đó, nếu không thì
        // lọc theo đúng một ngày sẽ không ra hồ sơ nào gửi sau 00:00 sáng.
        return ResponseEntity.ok(ApiResponses.success(applicationService.search(
                status,
                jobId,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.atTime(LocalTime.MAX),
                q,
                pageable)));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<RecruitmentDtos.ApplicationDetailDto>> detail(@PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponses.success(applicationService.getDetail(applicationId)));
    }

    @PatchMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<RecruitmentDtos.ApplicationDetailDto>> update(
            @PathVariable Long applicationId,
            @RequestBody RecruitmentDtos.ApplicationUpdateRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal == null ? null : principal.getStaffId();
        return ResponseEntity.ok(ApiResponses.success(
                applicationService.update(applicationId, request, staffId), "Đã cập nhật hồ sơ"));
    }

    /**
     * Gửi lại thư báo cho quản lý.
     *
     * <p>Có nút này vì lần gửi đầu có thể hỏng do cấu hình SMTP chưa xong; sửa
     * cấu hình rồi thì phải lấy lại được thư mà không cần nhờ ứng viên nộp lại.
     */
    @PostMapping("/{applicationId}/resend-notification")
    public ResponseEntity<ApiResponse<Void>> resendNotification(@PathVariable Long applicationId) {
        NotifyStatus result = mailService.notifyManagers(applicationId);
        return switch (result) {
            case SENT -> ResponseEntity.ok(ApiResponses.successMessage("Đã gửi lại thư báo hồ sơ"));
            case SKIPPED -> ResponseEntity.ok(ApiResponses.successMessage(
                    "Chưa gửi: thông báo email đang tắt hoặc chưa cấu hình hộp thư nhận"));
            default -> ResponseEntity.internalServerError().body(ApiResponses.error(
                    "MAIL_FAILED", "Gửi thư thất bại, xem lý do chi tiết ngay trên hồ sơ"));
        };
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long applicationId) {
        applicationService.delete(applicationId);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá hồ sơ"));
    }
}
