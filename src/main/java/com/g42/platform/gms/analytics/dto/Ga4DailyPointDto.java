package com.g42.platform.gms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Ga4DailyPointDto {
    private String date;
    private long activeUsers;
    private long sessions;
    private long pageViews;
}
