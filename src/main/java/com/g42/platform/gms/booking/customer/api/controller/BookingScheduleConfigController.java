package com.g42.platform.gms.booking.customer.api.controller;

import com.g42.platform.gms.booking.customer.api.dto.BookingScheduleConfigResponse;
import com.g42.platform.gms.booking.customer.application.service.BookingScheduleConfigService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cấu hình giờ hoạt động xưởng + tham số đặt lịch (độ dài slot, thời gian đặt tối thiểu,
 * số ngày cho đặt trước, sức chứa mặc định).
 *
 * - /api/admin/booking-schedule-config: đọc/sửa (trang Cấu hình hệ thống)
 * - /api/booking/schedule-config: đọc công khai, dùng cho luồng đặt lịch của khách/staff
 */
@RestController
@RequiredArgsConstructor
public class BookingScheduleConfigController {

    private final BookingScheduleConfigService bookingScheduleConfigService;

    @GetMapping("/api/admin/booking-schedule-config")
    public ResponseEntity<ApiResponse<BookingScheduleConfigResponse>> getConfigForAdmin() {
        return ResponseEntity.ok(ApiResponses.success(bookingScheduleConfigService.getConfig()));
    }

    @PutMapping("/api/admin/booking-schedule-config")
    public ResponseEntity<ApiResponse<BookingScheduleConfigResponse>> updateConfig(
            @RequestBody BookingScheduleConfigResponse dto) {
        return ResponseEntity.ok(ApiResponses.success(bookingScheduleConfigService.updateConfig(dto)));
    }

    @GetMapping("/api/booking/schedule-config")
    public ResponseEntity<ApiResponse<BookingScheduleConfigResponse>> getConfigPublic() {
        return ResponseEntity.ok(ApiResponses.success(bookingScheduleConfigService.getConfig()));
    }
}
