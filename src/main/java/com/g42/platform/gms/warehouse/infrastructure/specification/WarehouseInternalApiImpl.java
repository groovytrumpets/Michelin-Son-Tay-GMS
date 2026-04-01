package com.g42.platform.gms.warehouse.infrastructure.specification;

import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.api.internal.WarehouseInternalApi;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.mapper.CatalogItemJpaMapper;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WarehouseInternalApiImpl implements WarehouseInternalApi {
    @Autowired
    private CatalogItemJpaRepo catalogItemRepo;
    @Autowired
    private CatalogItemJpaMapper catalogItemJpaMapper;


    @Override
    public CatalogItemDto getItemInfo(Integer itemId) {
        CatalogItemJpa catalogItemJpa = catalogItemRepo.findById(itemId).orElse(null);
        return catalogItemJpaMapper.toDto(catalogItemJpa);
    }
}
