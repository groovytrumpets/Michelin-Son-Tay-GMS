package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.service_ticket_management.api.dto.assign.AvailableStaffDto;
import com.g42.platform.gms.service_ticket_management.application.service.TicketAssignmentService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.checkin.*;
import com.g42.platform.gms.service_ticket_management.application.service.CheckInService;
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * REST Controller for vehicle check-in process.
 * Handles receptionist operations for checking in vehicles.
 */
@Slf4j
@RestController
@RequestMapping("/api/receptionist/check-in")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;
    private final TicketAssignmentService ticketAssignmentService;

    /**
     * Tra cứu booking theo booking code — bước đầu tiên của check-in form.
     * Frontend gọi để hiển thị thông tin khách hàng, dịch vụ trước khi điền form.
     * POST /api/receptionist/check-in/lookup
     */
    @PostMapping("/lookup")
    public ResponseEntity<ApiResponse<BookingLookupResponse>> lookupBooking(@Valid @RequestBody BookingLookupRequest request) {
        try {
            BookingLookupResponse response = checkInService.lookupBooking(request);
            return ResponseEntity.ok(ApiResponses.success(response));
        } catch (CheckInException e) {
            log.error("Booking lookup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during booking lookup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponses.error("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }

    /**
     * Tạo xe mới cho khách hàng chưa có xe trong hệ thống.
     * POST /api/receptionist/check-in/vehicles/create
     */
    @PostMapping("/vehicles/create")
    public ResponseEntity<ApiResponse<CreateVehicleResponse>> createVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        try {
            CreateVehicleResponse response = checkInService.createVehicle(request);
            return ResponseEntity.ok(ApiResponses.success(response));
        } catch (CheckInException e) {
            log.error("Vehicle creation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during vehicle creation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponses.error("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }

    /**
     * Lấy danh sách advisor để hiển thị popup phân công khi check-in.
     * GET /api/receptionist/check-in/advisors
     */
    @GetMapping("/advisors")
    public ResponseEntity<ApiResponse<List<AvailableStaffDto>>> getAdvisors() {
        List<AvailableStaffDto> advisors = ticketAssignmentService.getAvailableStaff(0, "ADVISOR");
        return ResponseEntity.ok(ApiResponses.success(advisors));
    }

    /**
     * Get all vehicles of a customer.
     * GET /api/receptionist/check-in/customers/{customerId}/vehicles
     */
    @GetMapping("/customers/{customerId}/vehicles")
    public ResponseEntity<ApiResponse<CustomerVehiclesResponse>> getCustomerVehicles(@PathVariable Integer customerId) {
        try {
            CustomerVehiclesResponse response = checkInService.getCustomerVehicles(customerId);
            return ResponseEntity.ok(ApiResponses.success(response));
        } catch (CheckInException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error getting customer vehicles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponses.error("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }

    /**
     * Complete check-in in single page form (all-in-one).
     * POST /api/receptionist/check-in/complete-all
     */
    @PostMapping("/complete-all")
    public ResponseEntity<ApiResponse<ServiceTicketResponse>> completeCheckInAll(
            @Valid @ModelAttribute CompleteCheckInAllRequest request) {
        try {
            log.info("Received single-page check-in request for booking: {}", request.getBookingId());
            ServiceTicketResponse response = checkInService.completeCheckInAll(request);
            return ResponseEntity.ok(ApiResponses.success(response));
        } catch (CheckInException e) {
            log.error("Single-page check-in failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during single-page check-in", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponses.error("INTERNAL_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}
