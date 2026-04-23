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

    public List<ComboResDto> getListItemByCombo(Integer catalogId) {
        List<ComboResDto> apiResponse = comboItemRepo.getListItemByCatalog(catalogId).stream().map(comboItemDtoMapper::toDto).toList();
        return apiResponse;
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
        List<ComboItem> existingComboItems = comboItemRepo.getListItemByCatalog(catalogId);
        Map<Integer, ComboResDto> requestItemMap = comboResDto.stream().filter(Dto -> Dto.getComboItemId() !=null)
                .collect(Collectors.toMap(ComboResDto::getComboItemId,dto ->dto ));
        List<ComboItem> itemsToUpdate = new ArrayList<>();
        List<ComboItem> itemsToDelete = new ArrayList<>();

        //category for update or delete
        for (ComboItem comboItem : existingComboItems) {
            if (requestItemMap.containsKey(comboItem.getComboItemId())) {
                ComboResDto updateItem = requestItemMap.get(comboItem.getComboItemId());
                comboItem = comboItemDtoMapper.fromRestDto(updateItem);
                itemsToUpdate.add(comboItem);
            }else {
                itemsToDelete.add(comboItem);
            }
        }

        //category add new
        List<ComboItem> itemsToAdd = comboResDto.stream().filter(dto ->dto.getComboItemId()==null ).map(dto ->{
                ComboItem newItem = comboItemDtoMapper.toDomainRes(dto);
                newItem.setComboId(catalogId);
                return newItem;
                }).collect(Collectors.toList());

        if (!itemsToDelete.isEmpty()) {
            comboItemRepo.deleteAll(itemsToDelete);
        }

        comboItemRepo.saveListOfComboItems(itemsToUpdate);
        List<ComboItem> savedNewItems = comboItemRepo.saveListOfComboItems(itemsToAdd);
        List<ComboItem> finalItems = itemsToUpdate;
        finalItems.addAll(savedNewItems);
        return finalItems.stream().map(comboItemDtoMapper::toDto).toList();


    }
}
