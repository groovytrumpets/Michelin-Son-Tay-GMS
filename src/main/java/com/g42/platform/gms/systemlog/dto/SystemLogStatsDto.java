package com.g42.platform.gms.systemlog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogStatsDto {
    private long total;
    private long loginFailedCount;
    private long dataChangeCount;
}
