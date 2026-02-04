package com.g42.platform.gms.marketing.service_catalog.domain.entity;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;

import java.time.LocalDateTime;
import java.util.List;

public class Service {

    private Long serviceId;
    private boolean showPrice;
    private LocalDateTime displayFrom;
    private LocalDateTime displayTo;
    private String displayPrice;
    private String fullDescription;
    private String shortDescription;
    private String title;
    private ServiceStatus status;
    private List<ServiceMedia> media;

    public boolean isVisibleNow(LocalDateTime now) {
        if (status != ServiceStatus.ACTIVE) return false;
        if (displayFrom != null && now.isBefore(displayFrom)) return false;
        if (displayTo != null && now.isAfter(displayTo)) return false;
        return true;
    }

}
