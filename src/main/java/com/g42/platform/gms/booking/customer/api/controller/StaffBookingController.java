package com.g42.platform.gms.booking.customer.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.api.dto.StaffDirectBookingRequest;
import com.g42.platform.gms.booking.customer.api.mapper.BookingDtoMapper;
import com.g42.platform.gms.booking.customer.application.service.BookingService;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cho Staff/Receptionist tạo booking trực tiếp
 * 
 * Endpoint:
 * - POST /api/booking/staff/create-direct - Tạo booking trực tiếp (không qua BookingRequest)
 * 
 * Đặc điểm:
 * - Không bị ràng buộc thời gian 2 giờ
 * - Trạng thái CONFIRMED luôn
 * - Tự động tạo/tìm customer account từ phone
 */
@RestController
@RequestMapping("/api/booking/staff")
@RequiredArgsConstructor
public class StaffBookingController {

    private final BookingService bookingService;
    private final BookingDtoMapper dtoMapper;

    /**
     * Tạo booking trực tiếp cho khách hàng (bởi staff/receptionist)
     * 
     * Business Rules:
     * - Không bị ràng buộc thời gian 2 giờ (có thể đặt ngay)
     * - Trạng thái CONFIRMED luôn (không qua PENDING)
     * - Tự động tạo customer account nếu chưa có
     * - Check slot availability
     * 
     * @param request Thông tin booking (phone, fullName, appointmentDate, appointmentTime, userNote, selectedServiceIds)
     * @return Booking đã được tạo
     */
    @PostMapping("/create-direct")
//    @PreAuthorize("hasAnyRole('STAFF', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> createDirectBooking(
            @RequestBody @Valid StaffDirectBookingRequest request
    ) {
        Booking domain = bookingService.createDirectBookingByStaff(request);
        
        BookingResponse response = dtoMapper.toResponse(domain);
        
        // Populate customer info
        response.setPhone(request.getPhone());
        response.setCustomerName(request.getFullName());
        
        return ResponseEntity.ok(ApiResponses.success(response));
    }
}
