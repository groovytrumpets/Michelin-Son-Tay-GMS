package com.g42.platform.gms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchConsolePageDto {
    private String page;
    private long clicks;
    private long impressions;
    private double ctr;
    private double position;
}
