package com.g42.platform.gms.marketing.landingpage.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LandingPageConfigUpdateRequest {
    @Valid
    @NotNull
    private List<LandingPageSectionUpdateRequest> sections = new ArrayList<>();
}
