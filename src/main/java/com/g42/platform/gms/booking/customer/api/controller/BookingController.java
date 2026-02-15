package com.g42.platform.gms.booking.customer.api.controller;

import com.g42.platform.gms.auth.entity.CustomerPrincipal;
import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.api.dto.CustomerBookingRequest;
import com.g42.platform.gms.booking.customer.api.dto.ModifyBookingRequest;
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
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        // ✅ Lấy customerId từ principal
        Integer customerId = principal.getCustomerId();
        Booking domain = bookingService.createCustomerBooking(request, customerId);
        BookingResponse response = dtoMapper.toResponse(domain);
        
        // ✅ Enrich từ token (đã có trong principal), không cần query DB
        response.setCustomerName(principal.getName());
        response.setPhone(principal.getPhone());
        
        return ResponseEntity.ok(ApiResponses.success(response));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        Integer customerId = principal.getCustomerId();
        List<Booking> bookings = bookingService.getCustomerBookings(customerId);
        List<BookingResponse> responses = bookings.stream()
            .map(booking -> {
                BookingResponse response = dtoMapper.toResponse(booking);
                // ✅ Enrich từ token
                response.setCustomerName(principal.getName());
                response.setPhone(principal.getPhone());
                return response;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponses.success(responses));
    }

    @PutMapping("/{bookingId}/modify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> modifyBooking(
            @PathVariable Integer bookingId,
            @RequestBody @Valid ModifyBookingRequest request,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        Integer customerId = principal.getCustomerId();
        Booking domain = bookingService.modifyCustomerBooking(bookingId, request, customerId);
        BookingResponse response = dtoMapper.toResponse(domain);

        // ✅ Enrich từ token
        response.setCustomerName(principal.getName());
        response.setPhone(principal.getPhone());

        return ResponseEntity.ok(ApiResponses.success(response));
    }

    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @PathVariable Integer bookingId,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        Integer customerId = principal.getCustomerId();
        bookingService.cancelCustomerBooking(bookingId, customerId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
}
