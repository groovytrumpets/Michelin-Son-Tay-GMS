package com.g42.platform.gms.marketing.landingpage.api.dto;

import com.g42.platform.gms.marketing.landingpage.domain.LandingPageSection;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LandingPageSectionUpdateRequest {
    @NotNull
    private LandingPageSection section;

    @NotNull
    private List<Integer> catalogItemIds = new ArrayList<>();
}
