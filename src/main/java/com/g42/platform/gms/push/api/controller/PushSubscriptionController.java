package com.g42.platform.gms.push.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.push.api.dto.PushDeviceDto;
import com.g42.platform.gms.push.api.dto.PushSubscriptionRequest;
import com.g42.platform.gms.push.api.dto.PushUnsubscribeRequest;
import com.g42.platform.gms.push.application.service.PushSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quản lý Web Push subscription cho nhân viên đang đăng nhập.
 * Khớp contract FE: src/services/pushService.js.
 */
@RestController
@RequestMapping("/api/push/subscriptions")
public class PushSubscriptionController {

    @Autowired
    private PushSubscriptionService pushSubscriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> subscribe(
            @AuthenticationPrincipal StaffPrincipal staffPrincipal,
            @RequestBody PushSubscriptionRequest request) {
        pushSubscriptionService.save(staffPrincipal.getStaffId(), request);
        return ResponseEntity.ok(ApiResponses.success(true));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Boolean>> unsubscribe(
            @RequestBody PushUnsubscribeRequest request) {
        pushSubscriptionService.deactivate(request != null ? request.getEndpoint() : null);
        return ResponseEntity.ok(ApiResponses.success(true));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PushDeviceDto>>> myDevices(
            @AuthenticationPrincipal StaffPrincipal staffPrincipal) {
        return ResponseEntity.ok(ApiResponses.success(
                pushSubscriptionService.listDevices(staffPrincipal.getStaffId())));
    }
}
