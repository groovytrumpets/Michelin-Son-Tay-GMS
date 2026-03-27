package com.g42.platform.gms.marketing.service_catalog.api.dto;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class ServiceCreateRequest {
    private String title;
    private String shortDescription;
    private String fullDescription;
    private boolean showPrice;
    private String displayPrice;
    private ServiceStatus status;
    private Integer estimateTime;
    private MultipartFile thumbnailFile;
    private List<MultipartFile> mediaFiles;
}
