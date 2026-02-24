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
import jakarta.servlet.http.HttpServletRequest;
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
            @AuthenticationPrincipal CustomerPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        Integer customerId = principal.getCustomerId();
        String clientIp = getClientIp(httpRequest);
        Booking domain = bookingService.createCustomerBooking(request, customerId, clientIp);
        BookingResponse response = dtoMapper.toResponse(domain);
        
        // Populate customer info from token
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
                // Populate customer info from token
                response.setCustomerName(principal.getName());
                response.setPhone(principal.getPhone());
                return response;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponses.success(responses));
    }
    
    @GetMapping("/{identifier}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String identifier,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        Booking booking;
        
        // Check if identifier is a booking code (matches BK_XXXXXX pattern for random code)
        if (identifier.matches("^BK_[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{6}$")) {
            booking = bookingService.findByCode(identifier);
        } else {
            // It's a booking ID
            try {
                Integer bookingId = Integer.parseInt(identifier);
                booking = bookingService.findById(bookingId);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponses.error("INVALID_IDENTIFIER", "Mã booking không hợp lệ"));
            }
        }
        
        BookingResponse response = dtoMapper.toResponse(booking);
        response.setCustomerName(principal.getName());
        response.setPhone(principal.getPhone());
        
        return ResponseEntity.ok(ApiResponses.success(response));
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

        // Populate customer info from token
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
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
