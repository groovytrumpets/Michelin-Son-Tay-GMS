package com.g42.platform.gms.booking_management.api.controller;

import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedDetailResponse;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.api.dto.requesting.BookingRequestDetailRes;
import com.g42.platform.gms.booking_management.api.dto.requesting.BookingRequestRes;
import com.g42.platform.gms.booking_management.api.dto.requesting.ActionBookingRequest;
import com.g42.platform.gms.booking_management.application.service.BookingManageService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<List<BookingRequestRes>>> getAllBookingRequest(){
        List<BookingRequestRes> bookingRequestResList = bookingService.getListBookingRequest();
        return ResponseEntity.ok(ApiResponses.success(bookingRequestResList));
    }

    @GetMapping("/booking-request/{bookingId}")
    public ResponseEntity<ApiResponse<BookingRequestDetailRes>> geBookingRequestById(@PathVariable Integer bookingId){
        BookingRequestDetailRes bookingRequestDetailRes = bookingService.getBookingRequestById(bookingId);
        return  ResponseEntity.ok(ApiResponses.success(bookingRequestDetailRes));
    }
    @PostMapping("/booking-request/{requestId}/confirm")
    public ResponseEntity<ApiResponse<Boolean>> confirmBookingRequest(@PathVariable Integer requestId){
         return ResponseEntity.ok(ApiResponses.success(bookingService.confirmBookingRequest(requestId)));
    }
    @PutMapping("/booking-request/{requestId}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelBookingRequest(@PathVariable Integer requestId, @RequestBody ActionBookingRequest actionBookingRequest){
        return ResponseEntity.ok(ApiResponses.success(bookingService.cancelBookingRequest(requestId, actionBookingRequest)));
    }
    @PutMapping("/booking-request/{requestId}/spam")
    public ResponseEntity<ApiResponse<Boolean>> spamNotedBookingRequest(@PathVariable Integer requestId, @RequestBody ActionBookingRequest actionBookingRequest){
        return ResponseEntity.ok(ApiResponses.success(bookingService.spamNotedBookingRequest(requestId, actionBookingRequest)));
    }
//    @GetMapping("/booking-request/{bookingId}")
//    public ResponseEntity<ApiResponse<List<TimeSlot>>> getListTimeSlotByBookingId(@PathVariable Integer bookingId){
//        return ResponseEntity.ok(ApiResponses.success(bookingService.getListTimeSlotByBookingId(bookingId)));
//    }





}
