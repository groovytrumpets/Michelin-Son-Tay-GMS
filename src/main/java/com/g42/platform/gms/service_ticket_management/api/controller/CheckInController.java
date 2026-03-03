package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.service_ticket_management.api.dto.checkin.*;
import com.g42.platform.gms.service_ticket_management.application.service.CheckInService;
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Lookup booking by booking code.
     * Returns booking information with customer details and vehicle suggestions.
     * 
     * POST /api/receptionist/check-in/lookup
     */
    @PostMapping("/lookup")
    public ResponseEntity<?> lookupBooking(@Valid @RequestBody BookingLookupRequest request) {
        try {
            BookingLookupResponse response = checkInService.lookupBooking(request);
            return ResponseEntity.ok(response);
        } catch (CheckInException e) {
            log.error("Booking lookup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during booking lookup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }

    /**
     * Save or select vehicle for check-in.
     * If vehicleId provided, validate and return existing vehicle.
     * If vehicleId null, create new vehicle.
     * 
     * POST /api/receptionist/check-in/vehicle
     */
    @PostMapping("/vehicle")
    public ResponseEntity<?> saveVehicle(@Valid @RequestBody VehicleRequest request) {
        try {
            VehicleResponse response = checkInService.saveVehicle(request);
            return ResponseEntity.ok(response);
        } catch (CheckInException e) {
            log.error("Vehicle save failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during vehicle save", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }

    /**
     * Upload license plate photo.
     * 
     * POST /api/receptionist/check-in/photos/license-plate
     */
    @PostMapping("/photos/license-plate")
    public ResponseEntity<?> uploadLicensePlatePhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("vehicleId") Integer vehicleId) {
        try {
            PhotoUploadResponse response = checkInService.uploadLicensePlatePhoto(file, vehicleId);
            return ResponseEntity.ok(response);
        } catch (CheckInException e) {
            log.error("License plate photo upload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during license plate photo upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }

    /**
     * Upload vehicle condition photo.
     * 
     * POST /api/receptionist/check-in/photos/condition
     */
    @PostMapping("/photos/condition")
    public ResponseEntity<?> uploadConditionPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ticketCode") String ticketCode,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false) String description) {
        try {
            PhotoUploadRequest request = new PhotoUploadRequest();
            request.setTicketCode(ticketCode);
            request.setCategory(com.g42.platform.gms.service_ticket_management.domain.enums.PhotoCategory.valueOf(category));
            request.setDescription(description);
            
            PhotoUploadResponse response = checkInService.uploadConditionPhoto(file, request);
            return ResponseEntity.ok(response);
        } catch (CheckInException e) {
            log.error("Condition photo upload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid photo category: {}", category);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_CATEGORY", "Category không hợp lệ"));
        } catch (Exception e) {
            log.error("Unexpected error during condition photo upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }

    /**
     * Save odometer reading with rollback detection.
     * 
     * POST /api/receptionist/check-in/odometer
     */
    @PostMapping("/odometer")
    public ResponseEntity<?> saveOdometer(@Valid @RequestBody OdometerRequest request) {
        try {
            OdometerResponse response = checkInService.saveOdometer(request);
            return ResponseEntity.ok(response);
        } catch (CheckInException e) {
            log.error("Odometer save failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during odometer save", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }

    /**
     * Complete check-in process.
     * Finalizes service ticket creation and marks data as immutable.
     * 
     * POST /api/receptionist/check-in/complete
     */
    @PostMapping("/complete")
    public ResponseEntity<?> completeCheckIn(@Valid @RequestBody CompleteCheckInRequest request) {
        try {
            ServiceTicketResponse response = checkInService.completeCheckIn(request);
            return ResponseEntity.ok(response);
        } catch (CheckInException e) {
            log.error("Check-in completion failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during check-in completion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }

    /**
     * Error response DTO for consistent error handling.
     */
    private record ErrorResponse(String code, String message) {}

    /**
     * Get all vehicles of a customer.
     * Used for vehicle selection dropdown in check-in form.
     * 
     * GET /api/receptionist/check-in/customers/{customerId}/vehicles
     */
    @GetMapping("/customers/{customerId}/vehicles")
    public ResponseEntity<?> getCustomerVehicles(@PathVariable Integer customerId) {
        try {
            log.info("Getting vehicles for customer: {}", customerId);
            CustomerVehiclesResponse response = checkInService.getCustomerVehicles(customerId);
            return ResponseEntity.ok(response);
        } catch (CheckInException e) {
            log.error("Get customer vehicles failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error getting customer vehicles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }

    /**
     * Complete check-in in single page form (all-in-one).
     * This endpoint handles the entire check-in process in one API call:
     * - Select/create vehicle
     * - Upload all photos
     * - Save odometer reading
     * - Complete check-in
     * 
     * All operations are done in a single transaction.
     * 
     * POST /api/receptionist/check-in/complete-all
     */
    @PostMapping("/complete-all")
    public ResponseEntity<?> completeCheckInAll(
            @ModelAttribute CompleteCheckInAllRequest request) {
        try {
            log.info("Received single-page check-in request for booking: {}", request.getBookingId());
            ServiceTicketResponse response = checkInService.completeCheckInAll(request);
            return ResponseEntity.ok(response);
        } catch (CheckInException e) {
            log.error("Single-page check-in failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during single-page check-in", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

}
