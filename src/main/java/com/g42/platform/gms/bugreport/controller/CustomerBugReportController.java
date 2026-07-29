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
 * Bản sao endpoint báo lỗi dành cho khách hàng.
 * <p>
 * Token khách và token nhân viên ký bằng key khác nhau; {@code StaffJwtFilter}
 * trả 401 khi gặp token khách trên đường dẫn nó không bỏ qua. Prefix
 * {@code /api/customer/} nằm trong danh sách bỏ qua đó nên khách phải gọi qua
 * đây. Xử lý dùng chung {@link BugReportService} — danh tính vẫn lấy từ token.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer/bug-reports")
public class CustomerBugReportController {

    private final BugReportService bugReportService;

    @PostMapping
    public ResponseEntity<ApiResponse<BugReportDto>> create(@Valid @RequestBody BugReportCreateRequest request,
                                                            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponses.success(
                bugReportService.create(request, httpRequest),
                "Đã gửi báo lỗi. Cảm ơn bạn đã giúp chúng tôi cải thiện phần mềm!"));
    }
}
