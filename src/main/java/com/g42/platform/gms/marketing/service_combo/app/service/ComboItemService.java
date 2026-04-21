package com.g42.platform.gms.marketing.service_combo.app.service;

import com.g42.platform.gms.marketing.service_combo.api.dto.ComboCreateDto;
import com.g42.platform.gms.marketing.service_combo.api.dto.ComboResDto;
import com.g42.platform.gms.marketing.service_combo.api.mapper.ComboItemDtoMapper;
import com.g42.platform.gms.marketing.service_combo.domain.entity.ComboItem;
import com.g42.platform.gms.marketing.service_combo.domain.repository.ComboItemRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ComboItemService {
    @Autowired
    private ComboItemRepo comboItemRepo;
    @Autowired
    private ComboItemDtoMapper comboItemDtoMapper;

    public List<ComboResDto> getListItemByCombo(String catalogId) {
        List<ComboResDto> apiResponse = comboItemRepo.getListItemByCatalog(catalogId).stream().map(comboItemDtoMapper::toDto).toList();
        return apiResponse;
    }

    public List<ComboCreateDto> createListItemByCatalogId(List<ComboCreateDto> comboCreateDtos) {
        //todo: verify items input

        List<ComboItem> apiResponse = comboCreateDtos.stream().map(comboItemDtoMapper::toDomain).toList();
        return comboItemRepo.saveListOfComboItems(apiResponse).stream().map(comboItemDtoMapper::toCreateDto).toList();

    }
}
