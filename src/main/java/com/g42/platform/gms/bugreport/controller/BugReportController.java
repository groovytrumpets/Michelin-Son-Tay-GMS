package com.g42.platform.gms.bugreport.controller;

import com.g42.platform.gms.bugreport.dto.BugReportCreateRequest;
import com.g42.platform.gms.bugreport.dto.BugReportDto;
import com.g42.platform.gms.bugreport.service.BugReportService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gửi phiếu báo lỗi phần mềm. Mọi tài khoản đã đăng nhập (nhân viên hoặc khách hàng)
 * đều gọi được — danh tính người gửi lấy từ token, client không tự khai báo.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bug-reports")
public class BugReportController {

    private final BugReportService bugReportService;

    @PostMapping
    public ResponseEntity<ApiResponse<BugReportDto>> create(@Valid @RequestBody BugReportCreateRequest request,
                                                            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponses.success(
                bugReportService.create(request, httpRequest),
                "Đã gửi báo lỗi. Cảm ơn bạn đã giúp chúng tôi cải thiện phần mềm!"));
    }
}
