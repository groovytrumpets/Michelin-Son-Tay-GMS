package com.g42.platform.gms.marketing.service_combo.domain.repository;

import com.g42.platform.gms.marketing.service_combo.api.dto.ComboCreateDto;
import com.g42.platform.gms.marketing.service_combo.domain.entity.ComboItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboItemRepo {
    List<ComboItem> getListItemByCatalog(String catalogId);


    List<ComboItem> saveListOfComboItems(List<ComboItem> apiResponse);
}
