package com.g42.platform.gms.marketing.service_catalog.api.dto;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.ServiceMedia;
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
public class ServiceDetailRespond {
    private Long serviceId;
    private boolean showPrice;
    private String displayPrice;
    private String fullDescription;
    private String shortDescription;
    private String mediaThumbnail;
    private String title;
    private ServiceStatus status;
    private List<ServiceMedia> media;

}
