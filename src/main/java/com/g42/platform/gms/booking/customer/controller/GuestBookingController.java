package com.g42.platform.gms.booking.customer.controller;

import com.g42.platform.gms.booking.customer.dto.BookingRequestResponse;
import com.g42.platform.gms.booking.customer.dto.GuestBookingRequest;
import com.g42.platform.gms.booking.customer.service.BookingRequestService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking/guest")
@RequiredArgsConstructor
public class GuestBookingController {
    
    private final BookingRequestService bookingRequestService;
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<BookingRequestResponse>> createBookingRequest(
            @RequestBody @Valid GuestBookingRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = getClientIp(httpRequest);
        BookingRequestResponse response = bookingRequestService.createGuestRequest(request, clientIp);
        return ResponseEntity.ok(ApiResponses.success(response));
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
