package com.g42.platform.gms.manager.schedule.dto;

import lombok.Data;

@Data
public class WorkShiftStatsResponse {
    private long totalScheduledShifts;
    private long activeShiftsToday;
    private long pendingSwapRequests;
    private long completedShifts;
}
