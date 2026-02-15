package com.g42.platform.gms.booking_management.api.controller;

import com.g42.platform.gms.booking_management.api.dto.BookedDetailResponse;
import com.g42.platform.gms.booking_management.api.dto.BookedRespond;
import com.g42.platform.gms.booking_management.application.service.BookingManageService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/booking/manage")
public class BookingManageController {
    @Autowired
    private final BookingManageService bookingService;
    @GetMapping("/booking")
    public ResponseEntity<ApiResponse<List<BookedRespond>>> getAllBookings(){
        List<BookedRespond> apiResponse = bookingService.getListBooked();
        return ResponseEntity.ok(ApiResponses.success(apiResponse));
    }
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<BookedDetailResponse>> getBookedDetailById(@PathVariable Integer bookingId){
        BookedDetailResponse bookedDetailResponse = bookingService.getBookedDetailById(bookingId);
        return ResponseEntity.ok(ApiResponses.success(bookedDetailResponse));
    }
    @GetMapping("/booking-request")
    public ResponseEntity<ApiResponse<List<BookedRespond>>> getAllBookingRequest(){

        return null;
    }


}
