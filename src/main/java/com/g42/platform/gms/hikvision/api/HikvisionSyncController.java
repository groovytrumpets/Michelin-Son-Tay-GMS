package com.g42.platform.gms.hikvision.api;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.hikvision.sync.HikvisionSyncScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/manager/hikvision")
@RequiredArgsConstructor
public class HikvisionSyncController {

    private final HikvisionSyncScheduler scheduler;

    /**
     * Trigger manual sync from Hikvision device.
     * Useful for testing or catching up missed events.
     *
     * @param hoursBack how many hours back to sync (default 1)
     */
    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> triggerSync(
            @RequestParam(defaultValue = "1") int hoursBack) {
        scheduler.syncFrom(LocalDateTime.now().minusHours(hoursBack));
        return ResponseEntity.ok(ApiResponses.success(
                "Sync triggered for last " + hoursBack + " hour(s)"));
    }
}
