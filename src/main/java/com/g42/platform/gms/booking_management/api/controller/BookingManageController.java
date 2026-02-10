package com.g42.platform.gms.booking_management.api.controller;

import com.g42.platform.gms.booking_management.application.service.BookingManageService;
import com.g42.platform.gms.booking_management.domain.entity.Booking;
import com.g42.platform.gms.common.dto.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/book/handle")
public class BookingManageController {
    @Autowired
    private final BookingManageService bookingService;

    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings(){
        ApiResponse<List<Booking>> apiResponse = new ApiResponse<>();
        return  ResponseEntity.ok(apiResponse);
    }

}
