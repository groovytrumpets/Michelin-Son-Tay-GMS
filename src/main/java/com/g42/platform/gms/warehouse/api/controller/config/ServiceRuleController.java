package com.g42.platform.gms.warehouse.api.controller.config;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.request.CreateServiceRuleRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateServiceRuleRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ServiceSuggestion;
import com.g42.platform.gms.warehouse.app.service.rule.ServiceRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/service-rules")
@RequiredArgsConstructor
public class ServiceRuleController {

    private final ServiceRuleService serviceRuleService;

    @GetMapping("/suggest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ServiceSuggestion>>> suggest(
            @RequestParam String vehicleModel,
            @RequestParam int odometerKm) {
        return ResponseEntity.ok(ApiResponses.success(
                serviceRuleService.suggest(vehicleModel, odometerKm)));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ServiceSuggestion>> create(
            @Valid @RequestBody CreateServiceRuleRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                serviceRuleService.create(request, principal.getStaffId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ServiceSuggestion>> update(
            @PathVariable Integer id,
            @RequestBody UpdateServiceRuleRequest request) {
        return ResponseEntity.ok(ApiResponses.success(serviceRuleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        serviceRuleService.delete(id);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
}
