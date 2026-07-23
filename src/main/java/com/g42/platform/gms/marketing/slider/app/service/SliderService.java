package com.g42.platform.gms.marketing.slider.app.service;

import com.g42.platform.gms.marketing.slider.api.dto.SliderDto;
import com.g42.platform.gms.marketing.slider.api.dto.SliderItemDto;
import com.g42.platform.gms.marketing.slider.infrastructure.entity.SliderJpa;
import com.g42.platform.gms.marketing.slider.infrastructure.entity.SliderItemJpa;
import com.g42.platform.gms.marketing.slider.infrastructure.repository.SliderJpaRepo;
import com.g42.platform.gms.marketing.slider.infrastructure.repository.SliderItemJpaRepo;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import com.g42.platform.gms.promotion.infrastructure.repository.PromotionJpaRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SliderService {

    private final SliderJpaRepo sliderRepo;
    private final SliderItemJpaRepo sliderItemRepo;
    private final PromotionJpaRepo promotionRepo;

    public SliderService(SliderJpaRepo sliderRepo, SliderItemJpaRepo sliderItemRepo, PromotionJpaRepo promotionRepo) {
        this.sliderRepo = sliderRepo;
        this.sliderItemRepo = sliderItemRepo;
        this.promotionRepo = promotionRepo;
    }

    @Transactional(readOnly = true)
    public SliderDto getSliderByLocationCode(String locationCode) {
        SliderJpa slider = sliderRepo.findByLocationCode(locationCode)
                .orElseThrow(() -> new RuntimeException("Slider not found for location: " + locationCode));
        return mapToDto(slider);
    }

    @Transactional(readOnly = true)
    public List<SliderDto> getAllSliders() {
        return sliderRepo.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public SliderDto createSlider(SliderDto dto) {
        SliderJpa slider = new SliderJpa();
        slider.setName(dto.getName());
        slider.setLocationCode(dto.getLocationCode());
        slider.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return mapToDto(sliderRepo.save(slider));
    }

    @Transactional
    public SliderDto updateSlider(Integer id, SliderDto dto) {
        SliderJpa slider = sliderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Slider not found"));
        slider.setName(dto.getName());
        slider.setLocationCode(dto.getLocationCode());
        if (dto.getIsActive() != null) {
            slider.setIsActive(dto.getIsActive());
        }
        return mapToDto(sliderRepo.save(slider));
    }

    @Transactional
    public void deleteSlider(Integer id) {
        SliderJpa slider = sliderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Slider not found"));
        if (slider.getItems() != null && !slider.getItems().isEmpty()) {
            throw new RuntimeException("Không thể xóa Slider đang có banner. Vui lòng xóa hết banner bên trong trước.");
        }
        sliderRepo.delete(slider);
    }

    @Transactional
    public SliderItemDto addSliderItem(Integer sliderId, SliderItemDto itemDto) {
        SliderJpa slider = sliderRepo.findById(sliderId)
                .orElseThrow(() -> new RuntimeException("Slider not found"));

        SliderItemJpa item = new SliderItemJpa();
        item.setSlider(slider);
        item.setImageUrl(itemDto.getImageUrl());
        item.setTargetUrl(itemDto.getTargetUrl());
        item.setTitle(itemDto.getTitle());
        item.setSubtitle(itemDto.getSubtitle());
        item.setDisplayOrder(itemDto.getDisplayOrder());
        item.setIsActive(itemDto.getIsActive() != null ? itemDto.getIsActive() : true);

        if (itemDto.getPromotionId() != null) {
            PromotionJpa promotion = promotionRepo.findById(itemDto.getPromotionId())
                    .orElseThrow(() -> new RuntimeException("Promotion not found"));
            item.setPromotion(promotion);
        }

        return mapItemToDto(sliderItemRepo.save(item));
    }

    @Transactional
    public void deleteSliderItem(Integer itemId) {
        sliderItemRepo.deleteById(itemId);
    }

    @Transactional
    public SliderItemDto updateSliderItem(Integer sliderId, Integer itemId, SliderItemDto itemDto) {
        SliderItemJpa item = sliderItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Slider item not found"));
        
        if (!item.getSlider().getId().equals(sliderId)) {
            throw new RuntimeException("Slider item does not belong to this slider");
        }

        item.setImageUrl(itemDto.getImageUrl());
        item.setTargetUrl(itemDto.getTargetUrl());
        item.setTitle(itemDto.getTitle());
        item.setSubtitle(itemDto.getSubtitle());
        if (itemDto.getDisplayOrder() != null) {
            item.setDisplayOrder(itemDto.getDisplayOrder());
        }
        if (itemDto.getIsActive() != null) {
            item.setIsActive(itemDto.getIsActive());
        }

        if (itemDto.getPromotionId() != null) {
            PromotionJpa promotion = promotionRepo.findById(itemDto.getPromotionId())
                    .orElseThrow(() -> new RuntimeException("Promotion not found"));
            item.setPromotion(promotion);
        } else {
            item.setPromotion(null); // Clear promotion if null
        }

        return mapItemToDto(sliderItemRepo.save(item));
    }

    @Transactional
    public void reorderSliderItems(Integer sliderId, List<Integer> orderedItemIds) {
        SliderJpa slider = sliderRepo.findById(sliderId)
                .orElseThrow(() -> new RuntimeException("Slider not found"));

        for (int i = 0; i < orderedItemIds.size(); i++) {
            Integer itemId = orderedItemIds.get(i);
            SliderItemJpa item = sliderItemRepo.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Slider item not found with id: " + itemId));
            
            if (!item.getSlider().getId().equals(sliderId)) {
                throw new RuntimeException("Item " + itemId + " does not belong to slider " + sliderId);
            }
            
            item.setDisplayOrder(i + 1); // 1-based index for display order
            sliderItemRepo.save(item);
        }
    }

    private SliderDto mapToDto(SliderJpa slider) {
        SliderDto dto = new SliderDto();
        dto.setId(slider.getId());
        dto.setName(slider.getName());
        dto.setLocationCode(slider.getLocationCode());
        dto.setIsActive(slider.getIsActive());
        if (slider.getItems() != null) {
            dto.setItems(slider.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));
        }
        return dto;
    }

    private SliderItemDto mapItemToDto(SliderItemJpa item) {
        SliderItemDto dto = new SliderItemDto();
        dto.setId(item.getId());
        dto.setSliderId(item.getSlider().getId());
        if (item.getPromotion() != null) {
            dto.setPromotionId(item.getPromotion().getPromotionId());
        }
        dto.setImageUrl(item.getImageUrl());
        dto.setTargetUrl(item.getTargetUrl());
        dto.setTitle(item.getTitle());
        dto.setSubtitle(item.getSubtitle());
        dto.setDisplayOrder(item.getDisplayOrder());
        dto.setIsActive(item.getIsActive());
        return dto;
    }
}
