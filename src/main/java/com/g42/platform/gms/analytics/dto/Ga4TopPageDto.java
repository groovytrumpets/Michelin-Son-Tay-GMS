package com.g42.platform.gms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Ga4TopPageDto {
    private String pagePath;
    private String pageTitle;
    private long pageViews;
    private long activeUsers;
}
