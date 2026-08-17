package com.g42.platform.gms.marketing.recruitment.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.recruitment.api.dto.RecruitmentDtos;
import com.g42.platform.gms.marketing.recruitment.app.RecruitmentSettingService;
import com.g42.platform.gms.notification.infrastructure.EmailNotificationSender;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Cấu hình trang tuyển dụng và hộp thư nhận hồ sơ.
 *
 * <p>Chỉ MANAGER/ADMIN, chặt hơn hai controller còn lại của phân hệ: đổi địa
 * chỉ nhận hồ sơ là đổi nơi dữ liệu ứng viên được gửi tới, không phải việc
 * thường ngày của lễ tân.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/recruitment/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class AdminRecruitmentSettingController {

    private final RecruitmentSettingService settingService;

    @GetMapping
    public ResponseEntity<ApiResponse<RecruitmentDtos.SettingDto>> get() {
        return ResponseEntity.ok(ApiResponses.success(settingService.getForAdmin()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<RecruitmentDtos.SettingDto>> save(
            @Valid @RequestBody RecruitmentDtos.SettingDto request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal == null ? null : principal.getStaffId();
        try {
            return ResponseEntity.ok(ApiResponses.success(
                    settingService.save(request, staffId), "Đã lưu cấu hình tuyển dụng"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponses.error("INVALID_SETTING", e.getMessage()));
        }
    }

    /** Gửi thư thử để biết SMTP và danh sách người nhận có chạy hay không. */
    @PostMapping("/test-mail")
    public ResponseEntity<ApiResponse<Void>> sendTestMail(
            @Valid @RequestBody(required = false) RecruitmentDtos.TestMailRequest request) {
        try {
            settingService.sendTestMail(request == null ? null : request.to());
            return ResponseEntity.ok(ApiResponses.successMessage(
                    "Đã gửi thư kiểm tra. Kiểm tra hộp thư (kể cả mục Spam) trong vài phút tới."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponses.error("INVALID_RECIPIENT", e.getMessage()));
        } catch (EmailNotificationSender.MailDeliveryException e) {
            // Trả nguyên văn lý do của máy chủ SMTP: "Authentication failed" và
            // "Username and Password not accepted" dẫn tới hai cách sửa khác nhau.
            return ResponseEntity.internalServerError()
                    .body(ApiResponses.error("MAIL_FAILED", "Gửi thư thất bại: " + e.getMessage()));
        }
    }
}
