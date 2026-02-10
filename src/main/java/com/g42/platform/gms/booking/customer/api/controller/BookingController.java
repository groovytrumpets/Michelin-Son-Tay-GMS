package com.g42.platform.gms.booking.customer.api.controller;

import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.api.dto.CustomerBookingRequest;
import com.g42.platform.gms.booking.customer.api.mapper.BookingDtoMapper;
import com.g42.platform.gms.booking.customer.application.service.BookingService;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.exception.BookingException;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/booking/customer")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingDtoMapper dtoMapper;

    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @RequestBody @Valid CustomerBookingRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = extractToken(httpRequest);
        Booking domain = bookingService.createCustomerBooking(request, token);
        BookingResponse response = dtoMapper.toResponse(domain);
        return ResponseEntity.ok(ApiResponses.success(response));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            HttpServletRequest httpRequest
    ) {
        String token = extractToken(httpRequest);
        List<Booking> bookings = bookingService.getCustomerBookings(token);
        List<BookingResponse> responses = bookings.stream()
            .map(dtoMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponses.success(responses));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BookingException("Token không hợp lệ.");
        }
        return authHeader.substring(7);
    }
}
