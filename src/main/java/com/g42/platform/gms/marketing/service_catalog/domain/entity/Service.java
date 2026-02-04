package com.g42.platform.gms.marketing.service_catalog.domain.entity;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    private Long serviceId;
    private boolean showPrice;
    private LocalDateTime displayFrom;
    private LocalDateTime displayTo;
    private String displayPrice;
    private String fullDescription;
    private String shortDescription;
    private String thumbnailUrl;
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
