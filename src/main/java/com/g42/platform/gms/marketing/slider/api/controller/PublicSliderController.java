package com.g42.platform.gms.marketing.slider.api.controller;

import com.g42.platform.gms.marketing.slider.api.dto.SliderDto;
import com.g42.platform.gms.marketing.slider.app.service.SliderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/sliders")
public class PublicSliderController {

    private final SliderService sliderService;

    public PublicSliderController(SliderService sliderService) {
        this.sliderService = sliderService;
    }

    @GetMapping("/{locationCode}")
    public ResponseEntity<SliderDto> getSliderByLocation(@PathVariable String locationCode) {
        return ResponseEntity.ok(sliderService.getSliderByLocationCode(locationCode));
    }
}
