package com.g42.platform.gms.marketing.slider.api.dto;

import lombok.Data;

@Data
public class SliderItemDto {
    private Integer id;
    private Integer sliderId;
    private Integer promotionId;
    private String imageUrl;
    private String targetUrl;
    private String title;
    private String subtitle;
    private Integer displayOrder;
    private Boolean isActive;
}
