package com.g42.platform.gms.marketing.slider.api.controller;

import com.g42.platform.gms.marketing.slider.api.dto.SliderDto;
import com.g42.platform.gms.marketing.slider.api.dto.SliderItemDto;
import com.g42.platform.gms.marketing.slider.app.service.SliderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.g42.platform.gms.common.service.ImageUploadService;

@RestController
@RequestMapping("/api/admin/sliders")
public class AdminSliderController {

    private final SliderService sliderService;
    private final ImageUploadService imageUploadService;

    public AdminSliderController(SliderService sliderService, ImageUploadService imageUploadService) {
        this.sliderService = sliderService;
        this.imageUploadService = imageUploadService;
    }

    @GetMapping
    public ResponseEntity<List<SliderDto>> getAllSliders() {
        return ResponseEntity.ok(sliderService.getAllSliders());
    }

    @PostMapping
    public ResponseEntity<SliderDto> createSlider(@RequestBody SliderDto dto) {
        return ResponseEntity.ok(sliderService.createSlider(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SliderDto> updateSlider(@PathVariable Integer id, @RequestBody SliderDto dto) {
        return ResponseEntity.ok(sliderService.updateSlider(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlider(@PathVariable Integer id) {
        sliderService.deleteSlider(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sliderId}/items")
    public ResponseEntity<SliderItemDto> addSliderItem(
            @PathVariable Integer sliderId,
            @RequestBody SliderItemDto itemDto) {
        return ResponseEntity.ok(sliderService.addSliderItem(sliderId, itemDto));
    }

    @DeleteMapping("/{sliderId}/items/{itemId}")
    public ResponseEntity<Void> deleteSliderItem(
            @PathVariable Integer sliderId,
            @PathVariable Integer itemId) {
        sliderService.deleteSliderItem(itemId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{sliderId}/items/{itemId}")
    public ResponseEntity<SliderItemDto> updateSliderItem(
            @PathVariable Integer sliderId,
            @PathVariable Integer itemId,
            @RequestBody SliderItemDto itemDto) {
        return ResponseEntity.ok(sliderService.updateSliderItem(sliderId, itemId, itemDto));
    }

    @PutMapping("/{sliderId}/items/reorder")
    public ResponseEntity<Void> reorderSliderItems(
            @PathVariable Integer sliderId,
            @RequestBody List<Integer> orderedItemIds) {
        sliderService.reorderSliderItems(sliderId, orderedItemIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/upload-image")
    public ResponseEntity<java.util.Map<String, String>> uploadImage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String url = imageUploadService.uploadImage(file, "sliders");
            return ResponseEntity.ok(java.util.Map.of("url", url));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
