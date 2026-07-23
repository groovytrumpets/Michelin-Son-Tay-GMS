package com.g42.platform.gms.marketing.slider.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class SliderDto {
    private Integer id;
    private String name;
    private String locationCode;
    private Boolean isActive;
    private List<SliderItemDto> items;
}
