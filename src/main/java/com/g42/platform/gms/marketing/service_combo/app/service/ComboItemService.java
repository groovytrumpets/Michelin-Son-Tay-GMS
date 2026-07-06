package com.g42.platform.gms.marketing.service_combo.app.service;

import com.g42.platform.gms.marketing.service_combo.api.dto.ComboCreateDto;
import com.g42.platform.gms.marketing.service_combo.api.dto.ComboResDto;
import com.g42.platform.gms.marketing.service_combo.api.mapper.ComboItemDtoMapper;
import com.g42.platform.gms.marketing.service_combo.domain.entity.ComboItem;
import com.g42.platform.gms.marketing.service_combo.domain.repository.ComboItemRepo;
import com.g42.platform.gms.marketing.service_combo.infrastructure.mapper.ComboItemJpaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ComboItemService {
    @Autowired
    private ComboItemRepo comboItemRepo;
    @Autowired
    private ComboItemDtoMapper comboItemDtoMapper;

    public List<ComboResDto> getListItemByCombo(Integer catalogId, Integer odometerKm) {
        List<ComboItem> items = comboItemRepo.getListItemByCatalog(catalogId);
        if (odometerKm != null) {
            items = items.stream()
                    .filter(item -> item.getOdometerKm() == null || item.getOdometerKm() == 0 || item.getOdometerKm().equals(odometerKm))
                    .toList();
        }
        return items.stream().map(comboItemDtoMapper::toDto).toList();
    }

    public List<ComboCreateDto> createListItemByCatalogId(List<ComboCreateDto> comboCreateDtos, Integer catalogId) {
        //todo: verify items input

        List<ComboItem> apiResponse = comboCreateDtos.stream().map(comboCreateDto -> {
            ComboItem comboItem = comboItemDtoMapper.toDomain(comboCreateDto);
            comboItem.setComboId(catalogId);
            return comboItem;
        }).toList();
        return comboItemRepo.saveListOfComboItems(apiResponse).stream().map(comboItemDtoMapper::toCreateDto).toList();

    }
    @Transactional
    public List<ComboResDto> updateListItemByCatalogId(List<ComboResDto> comboResDto, Integer catalogId) {
        comboItemRepo.deleteByComboId(catalogId);

        List<ComboItem> itemsToAdd = comboResDto.stream().map(dto -> {
            ComboItem newItem = comboItemDtoMapper.toDomainRes(dto);
            newItem.setComboId(catalogId);
            newItem.setComboItemId(null);
            return newItem;
        }).collect(Collectors.toList());

        List<ComboItem> savedNewItems = comboItemRepo.saveListOfComboItems(itemsToAdd);
        return savedNewItems.stream().map(comboItemDtoMapper::toDto).toList();
    }
}
