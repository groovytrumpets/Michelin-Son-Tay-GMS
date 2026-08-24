package com.g42.platform.gms.catalogdraft.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.catalogdraft.api.dto.CatalogDraftRequest;
import com.g42.platform.gms.catalogdraft.api.dto.CatalogDraftResponse;
import com.g42.platform.gms.catalogdraft.application.service.CatalogDraftService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/catalog-drafts")
public class CatalogDraftController {
    private final CatalogDraftService service;

    public CatalogDraftController(CatalogDraftService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogDraftResponse>>> list(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestParam String type) {
        return ResponseEntity.ok(ApiResponses.success(service.list(principal.getStaffId(), type)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CatalogDraftResponse>> create(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestBody CatalogDraftRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.create(principal.getStaffId(), request)));
    }

    @PutMapping("/{draftId}")
    public ResponseEntity<ApiResponse<CatalogDraftResponse>> update(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Long draftId,
            @RequestBody CatalogDraftRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.update(principal.getStaffId(), draftId, request)));
    }

    @DeleteMapping("/{draftId}")
    public ResponseEntity<ApiResponse<Boolean>> delete(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Long draftId) {
        service.delete(principal.getStaffId(), draftId);
        return ResponseEntity.ok(ApiResponses.success(true));
    }
}
