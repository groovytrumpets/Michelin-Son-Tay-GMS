package com.g42.platform.gms.warehouse.infrastructure;

import com.g42.platform.gms.warehouse.api.dto.CatalogCreateDto;
import com.g42.platform.gms.warehouse.domain.entity.*;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.*;
import com.g42.platform.gms.warehouse.infrastructure.mapper.*;
import com.g42.platform.gms.warehouse.infrastructure.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CatalogItemRepoImpl implements CatalogItemRepo {
    @Autowired
    private CatalogItemJpaRepo catalogItemJpaRepo;
    @Autowired
    private BrandJpaRepo brandJpaRepo;
    @Autowired
    private SpecAttributeJpaRepo specAttributeJpaRepo;
    @Autowired
    private SpecificationJpaRepo specificationJpaRepo;
    @Autowired
    private ProductLineJpaRepo productLineJpaRepo;
    @Autowired
    private CatalogItemJpaMapper catalogItemJpaMapper;
    @Autowired
    private BrandJpaMapper brandJpaMapper;
    @Autowired
    private SpecAttributeJpaMapper specAttributeJpaMapper;
    @Autowired
    private ProductLineJpaMapper productLineJpaMapper;
    @Autowired
    private SpecificationJpaMapper specificationJpaMapper;


    @Override
    public List<Brand> getAllBrands() {
        List<BrandJpa> brandJpaList = brandJpaRepo.findAll();
        return brandJpaList.stream().filter(b -> b.getIsActive().equals((byte)1)).map(brandJpaMapper::toDomain).toList();
    }

    @Override
    public List<Specification> getAllSpecs() {
        List<SpecificationJpa> specificationJpaList = specificationJpaRepo.findAll();
        return specificationJpaList.stream().map(specificationJpaMapper::toDomain).toList();
    }

    @Override
    public List<ProductLine> getAllProductLines() {
        List<ProductLineJpa> specificationJpaList = productLineJpaRepo.findAll();
        return specificationJpaList.stream().filter(pl -> pl.getIsActive().equals((byte)1)).map(productLineJpaMapper::toDomain).toList();
    }

    @Override
    public List<SpecAttribute> getAllSpecAttibutes() {
        List<SpecAttributeJpa> specificationJpaList = specAttributeJpaRepo.findAll();
        return specificationJpaList.stream().map(specAttributeJpaMapper::toDomain).toList();
    }

    @Override
    public Brand createBrand(Brand brand) {
        BrandJpa brandJpa = brandJpaRepo.save(brandJpaMapper.toJpa(brand));
        return brandJpaMapper.toDomain(brandJpa);
    }

    @Override
    public Brand getBrandById(Integer brandId) {
        BrandJpa brandJpa = brandJpaRepo.findById(brandId).orElse(null);
        return brandJpaMapper.toDomain(brandJpa);
    }

    @Override
    public CatalogItem createCatalog(CatalogItem catalogItem) {
        CatalogItemJpa catalogItemJpa = catalogItemJpaRepo.save(catalogItemJpaMapper.toJpa(catalogItem));
        return catalogItemJpaMapper.toDomain(catalogItemJpa);
    }
}
