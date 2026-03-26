package com.g42.platform.gms.warehouse.app.service;

import com.g42.platform.gms.warehouse.api.dto.BrandHintDto;
import com.g42.platform.gms.warehouse.api.dto.ProductLineDto;
import com.g42.platform.gms.warehouse.api.dto.SpecAttributeDto;
import com.g42.platform.gms.warehouse.api.dto.SpecificationDto;
import com.g42.platform.gms.warehouse.api.mapper.BrandDtoMapper;
import com.g42.platform.gms.warehouse.api.mapper.ProductLineDtoMapper;
import com.g42.platform.gms.warehouse.api.mapper.SpecAttributeDtoMapper;
import com.g42.platform.gms.warehouse.api.mapper.SpecificationDtoMapper;
import com.g42.platform.gms.warehouse.domain.entity.*;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogItemService {
    @Autowired
    private CatalogItemRepo catalogItemRepo;
    @Autowired
    private BrandDtoMapper brandDtoMapper;
    @Autowired
    private SpecificationDtoMapper specificationDtoMapper;
    @Autowired
    private ProductLineDtoMapper productLineDtoMapper;
    @Autowired
    private SpecAttributeDtoMapper specAttributeDtoMapper;

    public List<BrandHintDto> getAllBrands() {
        List<Brand> brandList = catalogItemRepo.getAllBrands();
        return brandList.stream().map(brandDtoMapper::toDto).toList();
    }

    public List<SpecificationDto> getAllSpecs() {
        List<Specification> brandList = catalogItemRepo.getAllSpecs();
        return brandList.stream().map(specificationDtoMapper::toDto).toList();
    }

    public List<ProductLineDto> getAllProductLines() {
        List<ProductLine> brandList = catalogItemRepo.getAllProductLines();
        return brandList.stream().map(productLineDtoMapper::toDto).toList();
    }

    public List<SpecAttributeDto> getAllSpecAttributes() {
        List<SpecAttribute> brandList = catalogItemRepo.getAllSpecAttibutes();
        return brandList.stream().map(specAttributeDtoMapper::toDto).toList();
    }
}
