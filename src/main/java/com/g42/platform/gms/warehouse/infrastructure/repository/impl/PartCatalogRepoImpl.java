package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PartCatalogRepoImpl implements PartCatalogRepo {

    private final CatalogItemJpaRepo jpaRepo;

    @Override
    public List<CatalogItemJpa> findAll(Specification<CatalogItemJpa> spec) {
        return jpaRepo.findAll(spec);
    }

    @Override
    public List<CatalogItemJpa> findAllByIds(List<Integer> ids) {
        return jpaRepo.findAllById(ids);
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpaRepo.existsBySku(sku);
    }

    @Override
    public CatalogItemJpa save(CatalogItemJpa item) {
        return jpaRepo.save(item);
    }

    @Override
    public Map<Integer, String> findNamesByIds(List<Integer> ids) {
        return jpaRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(CatalogItemJpa::getItemId, CatalogItemJpa::getItemName));
    }
}
