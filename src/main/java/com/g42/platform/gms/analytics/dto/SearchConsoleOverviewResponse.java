package com.g42.platform.gms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchConsoleOverviewResponse {
    private long clicks;
    private long impressions;
    private double ctr;
    private double position;
    private List<SearchConsoleDailyPointDto> timeseries;
}
