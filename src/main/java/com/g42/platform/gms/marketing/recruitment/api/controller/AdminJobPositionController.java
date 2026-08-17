package com.g42.platform.gms.marketing.recruitment.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.marketing.recruitment.api.dto.RecruitmentDtos;
import com.g42.platform.gms.marketing.recruitment.app.JobPositionService;
import com.g42.platform.gms.marketing.recruitment.domain.EmploymentType;
import com.g42.platform.gms.marketing.recruitment.domain.JobStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/** Soạn và duyệt tin tuyển dụng. */
@Slf4j
@RestController
@RequestMapping("/api/admin/recruitment/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'RECEPTIONIST')")
public class AdminJobPositionController {

    private static final String IMAGE_FOLDER = "garage/recruitment";

    private final JobPositionService jobService;
    private final ImageUploadService imageUploadService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RecruitmentDtos.JobSummaryDto>>> search(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt", "jobId"));
        return ResponseEntity.ok(ApiResponses.success(
                jobService.searchForAdmin(status, department, employmentType, q, pageable)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<RecruitmentDtos.StatsDto>> stats() {
        return ResponseEntity.ok(ApiResponses.success(jobService.stats()));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<RecruitmentDtos.JobAdminDetailDto>> detail(@PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponses.success(jobService.getForEdit(jobId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecruitmentDtos.JobAdminDetailDto>> create(
            @Valid @RequestBody RecruitmentDtos.JobSaveRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal == null ? null : principal.getStaffId();
        return ResponseEntity.ok(ApiResponses.success(
                jobService.create(request, staffId), "Đã lưu tin tuyển dụng"));
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<ApiResponse<RecruitmentDtos.JobAdminDetailDto>> update(
            @PathVariable Long jobId,
            @Valid @RequestBody RecruitmentDtos.JobSaveRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                jobService.update(jobId, request), "Đã cập nhật tin tuyển dụng"));
    }

    /** Duyệt, trả lại, đóng hoặc lưu trữ tin. */
    @PatchMapping("/{jobId}/status")
    public ResponseEntity<ApiResponse<RecruitmentDtos.JobAdminDetailDto>> changeStatus(
            @PathVariable Long jobId,
            @RequestBody RecruitmentDtos.JobStatusChangeRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal == null ? null : principal.getStaffId();
        return ResponseEntity.ok(ApiResponses.success(
                jobService.changeStatus(jobId, request, staffId), "Đã cập nhật trạng thái"));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long jobId) {
        jobService.softDelete(jobId);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá tin tuyển dụng"));
    }

    /** Ảnh bìa và ảnh chèn trong nội dung tin. */
    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = imageUploadService.uploadImage(file, IMAGE_FOLDER);
            return ResponseEntity.ok(ApiResponses.success(Map.of("url", url)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponses.error("INVALID_FILE", e.getMessage()));
        } catch (IOException e) {
            log.error("Tải ảnh tin tuyển dụng lên Cloudinary thất bại", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponses.error("UPLOAD_FAILED", "Không tải được ảnh, vui lòng thử lại"));
        }
    }
}
