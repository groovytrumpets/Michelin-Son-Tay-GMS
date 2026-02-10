package com.g42.platform.gms.booking_management.api.controller;

import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking_management.api.dto.BookedRespond;
import com.g42.platform.gms.booking_management.application.service.BookingManageService;
import com.g42.platform.gms.booking_management.domain.entity.Booking;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/booking/manage")
public class BookingManageController {
    @Autowired
    private final BookingManageService bookingService;
    @GetMapping("/booked")
    public ResponseEntity<ApiResponse<List<BookedRespond>>> getAllBookings(){
        List<BookedRespond> apiResponse = bookingService.getListBooked();
        return ResponseEntity.ok(ApiResponses.success(apiResponse));
    }

}
