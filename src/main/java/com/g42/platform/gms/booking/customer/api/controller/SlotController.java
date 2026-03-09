package com.g42.platform.gms.booking.customer.api.controller;

import com.g42.platform.gms.booking.customer.api.dto.AvailableSlotsResponse;
import com.g42.platform.gms.booking.customer.api.dto.TimeSlotResponse;
import com.g42.platform.gms.booking.customer.application.service.SlotService;
import com.g42.platform.gms.booking.customer.domain.entity.TimeSlot;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller cho quản lý slots (khung giờ)
 * 
 * Endpoints:
 * - GET /api/booking/slots/available - Lấy slots cho customer (có filter 2 giờ)
 * - GET /api/booking/slots/available-for-staff - Lấy slots cho staff (không filter 2 giờ)
 * - GET /api/booking/slots/all - Lấy tất cả slot configs từ DB
 */
@RestController
@RequestMapping("/api/booking/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    /**
     * Lấy danh sách slots available cho CUSTOMER
     * 
     * Business Rule:
     * - Chỉ trả về slots sau 2 giờ từ hiện tại
     * - Chỉ trả về slots còn trống
     * - Yêu cầu authentication với role CUSTOMER
     * 
     * @param date Ngày cần check (format: yyyy-MM-dd)
     * @param durationMinutes Thời lượng ước tính (mặc định 60 phút)
     * @return Danh sách slots available
     */
    @GetMapping("/available")
//    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<AvailableSlotsResponse>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        List<TimeSlotResponse> slots = slotService.getAvailableSlotsForCustomer(date, durationMinutes);
        
        AvailableSlotsResponse response = new AvailableSlotsResponse();
        response.setDate(date);
        response.setSlots(slots);
        
        return ResponseEntity.ok(ApiResponses.success(response));
    }

    /**
     * Lấy danh sách slots available cho STAFF/RECEPTIONIST
     * 
     * Business Rule:
     * - Trả về slots từ hiện tại (có thể đặt ngay)
     * - Chỉ trả về slots còn trống
     * - Yêu cầu authentication với role STAFF, RECEPTIONIST, hoặc ADMIN
     * 
     * @param date Ngày cần check (format: yyyy-MM-dd)
     * @param durationMinutes Thời lượng ước tính (mặc định 60 phút)
     * @return Danh sách slots available
     */
    @GetMapping("/available-for-staff")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST')")
    public ResponseEntity<ApiResponse<AvailableSlotsResponse>> getAvailableSlotsForStaff(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        List<TimeSlotResponse> slots = slotService.getAvailableSlotsForStaff(date, durationMinutes);
        
        AvailableSlotsResponse response = new AvailableSlotsResponse();
        response.setDate(date);
        response.setSlots(slots);
        
        return ResponseEntity.ok(ApiResponses.success(response));
    }

    /**
     * Get all time slots from database (without date-specific availability)
     * @param activeOnly Optional parameter to filter only active slots (default: false)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getAllTimeSlots(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly
    ) {
        List<TimeSlot> timeSlots;
        
        if (Boolean.TRUE.equals(activeOnly)) {
            timeSlots = slotService.getActiveTimeSlots();
        } else {
            timeSlots = slotService.getAllTimeSlots();
        }
        
        List<TimeSlotResponse> responses = new ArrayList<>();
        for (TimeSlot slot : timeSlots) {
            TimeSlotResponse response = toResponse(slot);
            responses.add(response);
        }
        
        return ResponseEntity.ok(ApiResponses.success(responses));
    }

    private TimeSlotResponse toResponse(TimeSlot domain) {
        TimeSlotResponse response = new TimeSlotResponse();
        response.setSlotId(domain.getSlotId());
        response.setStartTime(domain.getStartTime());
        response.setCapacity(domain.getCapacity());
        response.setIsActive(domain.getIsActive());
        response.setPeriod(domain.getPeriod());
        return response;
    }
}
