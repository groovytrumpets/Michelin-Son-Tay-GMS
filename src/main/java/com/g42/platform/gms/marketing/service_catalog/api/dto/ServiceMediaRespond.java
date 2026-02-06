package com.g42.platform.gms.marketing.service_catalog.api.dto;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ServiceMediaRespond {
    private Long serviceMediaId;
    private int displayOrder;
    private String mediaDescription;
    private String mediaUrl;
    private MediaType mediaType;


}
