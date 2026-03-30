package com.g42.platform.gms.booking_management.api.controller;

import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedDetailResponse;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.api.dto.requesting.*;
import com.g42.platform.gms.booking_management.application.service.BookingManageService;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/booking/manage")
public class BookingManageController {
    @Autowired
    private final BookingManageService bookingService;
    @GetMapping("/booking")
    public ResponseEntity<ApiResponse<Page<BookedRespond>>> getAllBookings(@RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "10") int size,
                                                                           @RequestParam(required = false) LocalDate date,
                                                                           @RequestParam(required = false) Boolean isGuest,
                                                                           @RequestParam(required = false) BookingEnum status,
                                                                           @RequestParam(required = false) String search){
        Page<BookedRespond> apiResponse = bookingService.getListBooked(page,size,date,isGuest,status,search);
        return ResponseEntity.ok(ApiResponses.success(apiResponse));
    }
    @GetMapping("/booking/{bookingCode}")
    public ResponseEntity<ApiResponse<BookedDetailResponse>> getBookedDetailById(@PathVariable String bookingCode){
        BookedDetailResponse bookedDetailResponse = bookingService.getBookedDetailById(bookingCode);
        return ResponseEntity.ok(ApiResponses.success(bookedDetailResponse));
    }
    @GetMapping("/booking-request")
    public ResponseEntity<ApiResponse<Page<BookingRequestRes>>> getAllBookingRequest(@RequestParam(defaultValue = "0") int page,
                                                                                     @RequestParam(defaultValue = "10") int size,
                                                                                     @RequestParam(required = false) LocalDate date,
                                                                                     @RequestParam(required = false) Boolean isGuest,
                                                                                     @RequestParam(required = false) BookingRequestStatus status,
                                                                                     @RequestParam(required = false) String search){
        Page<BookingRequestRes> bookingRequestResList = bookingService.getListBookingRequest(page,size,date,isGuest,status,search);
        return ResponseEntity.ok(ApiResponses.success(bookingRequestResList));
    }

    @GetMapping("/booking-request/{bookingCode}")
    public ResponseEntity<ApiResponse<BookingRequestDetailRes>> geBookingRequestById(@PathVariable String bookingCode){
        BookingRequestDetailRes bookingRequestDetailRes = bookingService.getBookingRequestById(bookingCode);
        return  ResponseEntity.ok(ApiResponses.success(bookingRequestDetailRes));
    }
    @PostMapping("/booking-request/{requestId}/confirm")
    public ResponseEntity<ApiResponse<Boolean>> confirmBookingRequest(@PathVariable String requestId){
         return ResponseEntity.ok(ApiResponses.success(bookingService.confirmBookingRequest(requestId)));
    }
    @PutMapping("/booking-request/{requestId}/cancel")
    public ResponseEntity<ApiResponse<ActionBookingRespond>> cancelBookingRequest(@PathVariable String requestId, @RequestBody ActionBookingRequest actionBookingRequest){
        return ResponseEntity.ok(ApiResponses.success(bookingService.cancelBookingRequest(requestId, actionBookingRequest)));
    }
    @PutMapping("/booking-request/{requestId}/spam")
    public ResponseEntity<ApiResponse<ActionBookingRespond>> spamNotedBookingRequest(@PathVariable String requestId, @RequestBody ActionBookingRequest actionBookingRequest){
        return ResponseEntity.ok(ApiResponses.success(bookingService.spamNotedBookingRequest(requestId, actionBookingRequest)));
    }

    @PutMapping("/booking-request/{requestId}/contacted")
    public ResponseEntity<ApiResponse<ActionBookingRespond>> contactedBookingRequest(@PathVariable String requestId, @RequestBody ActionBookingRequest actionBookingRequest){
        return ResponseEntity.ok(ApiResponses.success(bookingService.contactedBookingRequest(requestId, actionBookingRequest)));
    }

    @PutMapping("/booking-request/{requestId}/update")
    public ResponseEntity<ApiResponse<Boolean>> updateBookingRequest(@PathVariable String requestId, @RequestBody BookingRequestUpdateReq actionBookingRequest){
        return ResponseEntity.ok(ApiResponses.success(bookingService.updateBookingRequest(requestId, actionBookingRequest)));
    }
    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<Boolean>> reorderQueue(@RequestBody ReorderQueueRequest request){
        return ResponseEntity.ok(ApiResponses.success(bookingService.reorderQueue(request)));
    }
    @GetMapping("/slot")
    public ResponseEntity<ApiResponse<List<BookedRespond>>> getBookingBySlot(
            @RequestParam LocalDate date,
            @RequestParam LocalTime slot) {
        return ResponseEntity.ok(ApiResponses.success(bookingService.getBookingBySlot(date, slot)));
    }
    @PutMapping("/set-queue")
    public ResponseEntity<ApiResponse<List<BookedRespond>>> setQueue(@RequestParam Integer bookingId,
                                                                     @RequestParam Integer queueNumber){
        return ResponseEntity.ok(ApiResponses.success(bookingService.setQueue(bookingId, queueNumber)));
    }
    @PutMapping("/set-queue-auto")
    public ResponseEntity<ApiResponse<List<BookedRespond>>> setQueueAuto(@RequestParam LocalDate date,
                                                                         @RequestParam LocalTime slot){
        return ResponseEntity.ok(ApiResponses.success(bookingService.setQueueAutoBySlotDate(date, slot)));
    }
    @PutMapping("/swap")
    public ResponseEntity<ApiResponse<List<BookedRespond>>> swapQueue(@RequestParam Integer bookingId1,
                                                                         @RequestParam Integer bookingId2){
        return ResponseEntity.ok(ApiResponses.success(bookingService.setswapQueueByBookingIds(bookingId1, bookingId2)));
    }
//
//    @GetMapping("/booking-request")
//    public ResponseEntity<ApiResponse<Page<BookingRequestRes>>> getAllBookingSlot(@RequestParam(required = false) LocalDate date){
////        Page<BookingRequestRes> bookingRequestResList = bookingService.getListBookingRequest(page,size,date,isGuest,status,search);
////        return ResponseEntity.ok(ApiResponses.success(bookingRequestResList));
//        return null;
//    }







}
