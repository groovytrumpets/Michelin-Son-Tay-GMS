package com.g42.platform.gms.marketing.recruitment.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.recruitment.api.dto.RecruitmentDtos;
import com.g42.platform.gms.marketing.recruitment.app.CvUploadService;
import com.g42.platform.gms.marketing.recruitment.app.JobApplicationService;
import com.g42.platform.gms.marketing.recruitment.app.JobPositionService;
import com.g42.platform.gms.marketing.recruitment.app.RecruitmentSettingService;
import com.g42.platform.gms.marketing.recruitment.domain.EmploymentType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/** API trang tuyển dụng dành cho khách, không cần đăng nhập. */
@Slf4j
@RestController
@RequestMapping("/api/public/recruitment")
@RequiredArgsConstructor
public class PublicRecruitmentController {

    private final JobPositionService jobService;
    private final JobApplicationService applicationService;
    private final RecruitmentSettingService settingService;
    private final CvUploadService cvUploadService;

    /** Nội dung đầu trang + danh sách bộ phận để đổ vào ô lọc. */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<RecruitmentDtos.PublicPageDto>> page() {
        return ResponseEntity.ok(ApiResponses.success(settingService.getPublicPage()));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<Page<RecruitmentDtos.JobSummaryDto>>> jobs(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        // Thứ tự do defaultPublicOrder() đặt bằng Criteria API, nên Pageable ở
        // đây cố tình không mang Sort — truyền thêm sẽ chồng lên và làm hỏng nó.
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return ResponseEntity.ok(ApiResponses.success(
                jobService.searchForPublic(department, employmentType, q, pageable)));
    }

    @GetMapping("/jobs/{slug}")
    public ResponseEntity<ApiResponse<RecruitmentDtos.JobDetailDto>> detail(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponses.success(jobService.getPublicDetail(slug)));
    }

    /** Nhận hồ sơ ứng tuyển. */
    @PostMapping("/jobs/{slug}/apply")
    public ResponseEntity<ApiResponse<RecruitmentDtos.ApplicationReceiptDto>> apply(
            @PathVariable String slug,
            @Valid @RequestBody RecruitmentDtos.ApplicationSubmitRequest request,
            HttpServletRequest httpRequest) {
        try {
            RecruitmentDtos.ApplicationReceiptDto receipt =
                    applicationService.submit(slug, request, clientIp(httpRequest));
            return ResponseEntity.ok(ApiResponses.success(receipt, receipt.message()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponses.error("APPLY_REJECTED", e.getMessage()));
        }
    }

    /** Tải file CV lên trước, rồi gửi kèm đường dẫn trong biểu mẫu ứng tuyển. */
    @PostMapping("/upload-cv")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCv(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(ApiResponses.success(Map.of("url", cvUploadService.upload(file))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponses.error("INVALID_FILE", e.getMessage()));
        } catch (IOException e) {
            log.error("Tuyển dụng: tải CV lên Cloudinary thất bại", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponses.error("UPLOAD_FAILED", "Không tải được file, vui lòng thử lại"));
        }
    }

    /**
     * Địa chỉ người gửi. Ứng dụng chạy sau nginx nên IP thật nằm ở
     * X-Forwarded-For; phần tử đầu tiên là máy khách, các phần sau là proxy.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
