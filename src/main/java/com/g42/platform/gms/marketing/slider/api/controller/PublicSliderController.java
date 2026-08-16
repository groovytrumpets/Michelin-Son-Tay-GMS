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

    /** Vị trí chưa cấu hình ảnh trả về 204 để giao diện tự dùng ảnh mặc định. */
    @GetMapping("/{locationCode}")
    public ResponseEntity<SliderDto> getSliderByLocation(@PathVariable String locationCode) {
        return sliderService.findSliderByLocationCode(locationCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
