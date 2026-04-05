package com.g42.platform.gms.hikvision.api;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.hikvision.sync.HikvisionSyncScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/manager/hikvision")
@RequiredArgsConstructor
public class HikvisionSyncController {

    private final HikvisionSyncScheduler scheduler;

    /**
     * Trigger manual sync for a specific date.
     * Defaults to today if no date provided.
     *
     * @param date date to sync in format yyyy-MM-dd (default: today)
     */
    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> triggerSync(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate syncDate = (date != null) ? date : LocalDate.now();
        scheduler.syncDate(syncDate);
        return ResponseEntity.ok(ApiResponses.success("Sync triggered for date: " + syncDate));
    }
}
