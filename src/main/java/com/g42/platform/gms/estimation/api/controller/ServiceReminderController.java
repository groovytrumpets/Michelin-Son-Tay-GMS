package com.g42.platform.gms.estimation.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.estimation.api.dto.RemindReason;
import com.g42.platform.gms.estimation.api.dto.ReminderCreateDto;
import com.g42.platform.gms.estimation.api.dto.ReminderRespondDto;
import com.g42.platform.gms.estimation.app.service.ReminderService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<ReminderRespondDto>> findReminderByServiceTicket(@PathVariable Integer serviceTicketId){
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



}
