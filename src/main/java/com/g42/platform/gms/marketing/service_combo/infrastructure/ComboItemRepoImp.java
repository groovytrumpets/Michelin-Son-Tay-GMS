package com.g42.platform.gms.marketing.service_combo.infrastructure;

import com.g42.platform.gms.marketing.service_combo.api.dto.ComboCreateDto;
import com.g42.platform.gms.marketing.service_combo.api.dto.ComboResDto;
import com.g42.platform.gms.marketing.service_combo.domain.entity.ComboItem;
import com.g42.platform.gms.marketing.service_combo.domain.repository.ComboItemRepo;
import com.g42.platform.gms.marketing.service_combo.infrastructure.entity.ComboItemJpa;
import com.g42.platform.gms.marketing.service_combo.infrastructure.mapper.ComboItemJpaMapper;
import com.g42.platform.gms.marketing.service_combo.infrastructure.repository.ComboItemRepoJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ComboItemRepoImp implements ComboItemRepo {
    @Autowired
    private ComboItemJpaMapper comboItemJpaMapper;
    @Autowired
    private ComboItemRepoJpa comboItemRepoJpa;

    public ComboItemRepoImp(ComboItemJpaMapper comboItemJpaMapper) {
        this.comboItemJpaMapper = comboItemJpaMapper;
    }

    @Override
    public List<ComboItem> getListItemByCatalog(Integer catalogId) {
        List<ComboItemJpa> comboItemJpas = comboItemRepoJpa.findAllByComboId(catalogId);
        return comboItemRepoJpa.saveAll(comboItemJpas).stream().map(comboItemJpaMapper::toDomain).toList();
    }

    @Override
    public List<ComboItem> saveListOfComboItems(List<ComboItem> comboCreateDtos) {
        List<ComboItemJpa> comboItemJpas = comboCreateDtos.stream().map(comboItemJpaMapper::toJpa).toList();
        return comboItemRepoJpa.saveAll(comboItemJpas).stream().map(comboItemJpaMapper::toDomain).toList();

    }

    @Override
    public void deleteAll(List<ComboItem> itemsToDelete) {
        List<ComboItemJpa> comboItemJpas = itemsToDelete.stream().map(comboItemJpaMapper::toJpa).toList();
        comboItemRepoJpa.deleteAll(comboItemJpas);
    }
}
