package com.g42.platform.gms.estimation.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.estimation.api.dto.RemindReason;
import com.g42.platform.gms.estimation.api.dto.RemindSearchDto;
import com.g42.platform.gms.estimation.api.dto.ReminderCreateDto;
import com.g42.platform.gms.estimation.api.dto.ReminderRespondDto;
import com.g42.platform.gms.estimation.app.service.ReminderService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/service-ticket/remind")
public class ServiceReminderController {
    private final ReminderService reminderService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ReminderRespondDto>> createReminder(@RequestBody ReminderCreateDto request,@AuthenticationPrincipal StaffPrincipal principal){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponses.success(reminderService.createReminder(request,principal))
        );
    }
    @GetMapping("/{serviceTicketId}")
    public ResponseEntity<ApiResponse<List<ReminderRespondDto>>> findReminderByServiceTicket(@PathVariable Integer serviceTicketId){
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponses.success(reminderService.findReminderByServiceTicket(serviceTicketId))
        );
    }
    @GetMapping("/find")
    public ResponseEntity<ApiResponse<List<ReminderRespondDto>>> findReminderByCustomerOrVehicle(@RequestParam(required = false) Integer customerId, @RequestParam(required = false) Integer vehicleId){
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponses.success(reminderService.findReminderByCustomerOrVehicle(customerId,vehicleId))
        );
    }
    @PatchMapping("/{remindId}/skipped")
    public ResponseEntity<ApiResponse<ReminderRespondDto>> updateSkippedRemind(@PathVariable Integer remindId,@RequestBody(required = false) RemindReason reason){
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponses.success(reminderService.updateSkippedRemind(remindId,reason))
        );
    }
    @PatchMapping("/{remindId}/confirmed")
    public ResponseEntity<ApiResponse<ReminderRespondDto>> updateConfirmedRemind(@PathVariable Integer remindId,@RequestBody(required = false) RemindReason reason){
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponses.success(reminderService.updateConfirmedRemind(remindId,reason))
        );
    }
    @PatchMapping("/{remindId}/cancelled")
    public ResponseEntity<ApiResponse<ReminderRespondDto>> updateCancelledRemind(@PathVariable Integer remindId,@RequestBody(required = false) RemindReason reason){
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponses.success(reminderService.updateCancelledRemind(remindId,reason))
        );
    }
    @PatchMapping("/{remindId}/notified")
    public ResponseEntity<ApiResponse<ReminderRespondDto>> updateNotifiedRemind(@PathVariable Integer remindId,@RequestBody(required = false) RemindReason reason){
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponses.success(reminderService.updateNotifiedRemind(remindId,reason))
        );
    }
    @GetMapping("/service-remind-search")
    public ResponseEntity<ApiResponse<Page<RemindSearchDto>>> searchReminders(@RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "10") int size,
                                                                              @RequestParam(required = false) LocalDateTime date,
                                                                              @RequestParam(required = false) String status,
                                                                              @RequestParam(required = false) String search,
                                                                              @RequestParam(required = false) String sortBy){
        Page<RemindSearchDto> apiResponse = reminderService.searchReminders(page,size,date,status,search,sortBy);
        return ResponseEntity.ok(ApiResponses.success(apiResponse));
    }



}
