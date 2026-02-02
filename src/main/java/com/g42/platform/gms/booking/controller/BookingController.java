package com.g42.platform.gms.booking.controller;

import com.g42.platform.gms.booking.dto.BookingRequest;
import com.g42.platform.gms.booking.service.BookingService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking")
public class BookingController {

    @Autowired private BookingService bookingService;

    @PostMapping("/create")
    public ApiResponse<Object> createBooking(@RequestBody @Valid BookingRequest request) {
        var result = bookingService.createBooking(request);
        return ApiResponses.success(result);
    }
}