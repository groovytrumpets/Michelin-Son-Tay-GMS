package com.g42.platform.gms.dashboard.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.dashboard.api.dto.NotificationCreateDto;
import com.g42.platform.gms.dashboard.api.dto.NotificationRespondDto;
import com.g42.platform.gms.dashboard.application.service.StaffNotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff-notification")
public class StaffNotifyController {
    @Autowired
    private StaffNotifyService staffNotifyService;

    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> notifyStaff(@RequestBody NotificationCreateDto dto){
        staffNotifyService.createAndSendManual(dto);
        return ResponseEntity.ok(ApiResponses.success(true));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationRespondDto>>> getMyNotification(@AuthenticationPrincipal StaffPrincipal staffPrincipal){
        return ResponseEntity.ok(ApiResponses.success(staffNotifyService.getMyNotifications(staffPrincipal.getStaffId())));
    }
    @PutMapping("{notificationId}/isReaded")
    public ResponseEntity<ApiResponse<Boolean>> markAsRead(@PathVariable Integer notificationId){
        staffNotifyService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponses.success(true));
    }

}
