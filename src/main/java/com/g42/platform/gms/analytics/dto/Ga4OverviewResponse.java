package com.g42.platform.gms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Ga4OverviewResponse {
    private long activeUsers;
    private long newUsers;
    private long sessions;
    private long pageViews;
    private double averageSessionDurationSec;
    private double bounceRate;
    private double engagementRate;
    private List<Ga4DailyPointDto> timeseries;
}
