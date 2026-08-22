package com.g42.platform.gms.marketing.landingpage.api.dto;

import com.g42.platform.gms.marketing.landingpage.domain.LandingPageSection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LandingPageSectionDto {
    private LandingPageSection section;
    private int maxItems;
    private boolean configured;
    private List<Integer> catalogItemIds;
}
