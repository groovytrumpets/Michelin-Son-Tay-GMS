package com.g42.platform.gms.booking.customer.api.controller;

import com.g42.platform.gms.booking.customer.api.dto.AvailableSlotResponse;
import com.g42.platform.gms.booking.customer.api.dto.AvailableSlotsResponse;
import com.g42.platform.gms.booking.customer.application.service.SlotService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/booking/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<AvailableSlotsResponse>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        List<AvailableSlotResponse> slots = slotService.getAvailableSlotsForCustomer(date, durationMinutes);
        
        AvailableSlotsResponse response = new AvailableSlotsResponse();
        response.setDate(date);
        response.setSlots(slots);
        
        return ResponseEntity.ok(ApiResponses.success(response));
    }
}
