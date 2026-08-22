package com.g42.platform.gms.marketing.landingpage.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LandingPageConfigDto {
    private List<LandingPageSectionDto> sections;
}
