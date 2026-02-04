package com.g42.platform.gms.marketing.service_catalog.api.dto;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ServiceSumaryRespond {
    private Long serviceId;
    private String title;
    private String shortDescription;
    private boolean showPrice;
    private String displayPrice;
    private String thumbnailUrl;
    private ServiceStatus status;

}
