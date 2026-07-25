package com.g42.platform.gms.docs.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.docs.api.dto.DocsProgressDto;
import com.g42.platform.gms.docs.api.dto.DocsProgressUpdateDto;
import com.g42.platform.gms.docs.api.dto.ManagerDocsReportDto;
import com.g42.platform.gms.docs.app.service.DocsProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/docs/progress")
public class DocsProgressController {

    private final DocsProgressService docsProgressService;

    @GetMapping("/{staffId}")
    public ResponseEntity<ApiResponse<List<DocsProgressDto>>> getStaffProgress(@PathVariable Integer staffId) {
        return ResponseEntity.ok(ApiResponses.success(docsProgressService.getProgressByStaffId(staffId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DocsProgressDto>> updateProgress(@RequestBody DocsProgressUpdateDto updateDto) {
        return ResponseEntity.ok(ApiResponses.success(docsProgressService.updateProgress(updateDto)));
    }

    @GetMapping("/manager-report")
    public ResponseEntity<ApiResponse<List<ManagerDocsReportDto>>> getManagerReport(@RequestParam(defaultValue = "18") int totalTopics) {
        return ResponseEntity.ok(ApiResponses.success(docsProgressService.getManagerReport(totalTopics)));
    }
}
