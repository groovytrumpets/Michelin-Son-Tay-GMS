package com.g42.platform.gms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Ga4TrafficSourceDto {
    private String channel;
    private long sessions;
    private long activeUsers;
}
