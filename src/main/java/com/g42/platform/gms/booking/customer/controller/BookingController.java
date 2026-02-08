package com.g42.platform.gms.booking.customer.controller;

import com.g42.platform.gms.booking.customer.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.dto.CustomerBookingRequest;
import com.g42.platform.gms.booking.customer.exception.BookingException;
import com.g42.platform.gms.booking.customer.service.BookingService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking/customer")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @RequestBody @Valid CustomerBookingRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = extractToken(httpRequest);
        BookingResponse response = bookingService.createCustomerBooking(request, token);
        return ResponseEntity.ok(ApiResponses.success(response));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            HttpServletRequest httpRequest
    ) {
        String token = extractToken(httpRequest);
        List<BookingResponse> bookings = bookingService.getCustomerBookings(token);
        return ResponseEntity.ok(ApiResponses.success(bookings));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BookingException("Token không hợp lệ.");
        }
        return authHeader.substring(7);
    }
}