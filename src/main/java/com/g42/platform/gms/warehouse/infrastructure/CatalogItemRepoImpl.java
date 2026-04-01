package com.g42.platform.gms.warehouse.infrastructure;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.repository.ServiceJpaRepository;
import com.g42.platform.gms.warehouse.api.dto.SpecificationRespondDto;
import com.g42.platform.gms.warehouse.domain.entity.*;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.*;
import com.g42.platform.gms.warehouse.infrastructure.mapper.*;
import com.g42.platform.gms.warehouse.infrastructure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @Autowired
    private ServiceJpaRepository serviceJpaRepository;
    @Autowired
    private WorkCategoryEntityJpaMapper itemCategoryJpaMapper;
    @Autowired
    private WorkCategoryJpaEntityRepo itemCategoryJpaRepo;


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


    @Override
    public ProductLine saveProductLine(ProductLine productLine) {
        ProductLineJpa productLineJpa = productLineJpaRepo.save(productLineJpaMapper.toJpa(productLine));
        return  productLineJpaMapper.toDomain(productLineJpa);
    }

    @Override
    public boolean exitBySku(String sku) {
        return catalogItemJpaRepo.existsBySku(sku);
    }
    @Override
    @Transactional
    public WorkCategory saveItemCate(WorkCategory itemCategory) {

        WorkCategoryJpaEntity itemCategoryJpa = itemCategoryJpaRepo.save(itemCategoryJpaMapper.toJpa(itemCategory));
        return itemCategoryJpaMapper.toDomain(itemCategoryJpa);
    }

    @Override
    public ProductLine getProductLineById(Integer productLineId) {
        ProductLineJpa itemJpa = productLineJpaRepo.findById(productLineId).orElse(null);
        return productLineJpaMapper.toDomain(itemJpa);
    }

    @Override
    public List<Specification> getListOfSpecsByItem(Integer itemId) {
        List<SpecificationJpa> specificationJpas = specificationJpaRepo.findAllByItemId(itemId);
        return specificationJpas.stream().map(specificationJpaMapper::toDomain).toList();
    }

    @Override
    public WorkCategory getItemCategoryById(Integer itemCategoryId) {
        WorkCategoryJpaEntity itemCategoryJpa = itemCategoryJpaRepo.findById(itemCategoryId).orElse(null);
        return itemCategoryJpaMapper.toDomain(itemCategoryJpa);
    }

    @Override
    public CatalogItem saveCatalogItem(CatalogItem catalogItem) {
        CatalogItemJpa catalogItemJpa = catalogItemJpaRepo.save(catalogItemJpaMapper.toJpa(catalogItem));
        return catalogItemJpaMapper.toDomain(catalogItemJpa);
    }

    @Override
    public boolean exitByCategoryCode(String categoryCode) {
        return itemCategoryJpaRepo.existsByCategoryCode(categoryCode);
    }

    @Override
    public Specification saveSpec(Specification specification) {
        SpecificationJpa specificationJpa = specificationJpaRepo.save(specificationJpaMapper.toJpa(specification));
        return specificationJpaMapper.toDomain(specificationJpa);
    }

    @Override
    public SpecAttribute saveSpecAttribute(SpecAttribute specAttribute) {
        SpecAttributeJpa specAttributeJpa = specAttributeJpaRepo.save(specAttributeJpaMapper.toJpa(specAttribute));
        return specAttributeJpaMapper.toDomain(specAttributeJpa);
    }

    @Override
    public List<WorkCategory> getAllItemCategory() {
        List<WorkCategoryJpaEntity> itemCategoryJpas = itemCategoryJpaRepo.findAll();
        return itemCategoryJpas.stream().map(itemCategoryJpaMapper::toDomain).toList();
    }

    @Override
    public SpecAttribute getSpecAttributeById(Integer attributeId) {
        SpecAttributeJpa specAttributeJpa = specAttributeJpaRepo.findById(attributeId).orElse(null);
        return specAttributeJpaMapper.toDomain(specAttributeJpa);
    }

    @Override
    public CatalogItem getCatalogItemById(Integer itemId) {
        CatalogItemJpa catalogItemJpa = catalogItemJpaRepo.findById(itemId).orElse(null);
        return catalogItemJpaMapper.toDomain(catalogItemJpa);
    }
    @Override
    public Integer findCategoryCode(String categoryCode) {
        WorkCategoryJpaEntity workCategoryJpaEntity = itemCategoryJpaRepo.findByCategoryCode(categoryCode);
        if (workCategoryJpaEntity == null) {
            return null;
        }
        return workCategoryJpaEntity.getWorkCategoryId();
    }

    @Override
    public Map<Integer, String> getAllBrandByIds(Set<Integer> brandIds) {
        return brandJpaRepo.getBrandMapByIds(brandIds);
    }

    @Override
    public Map<Integer, String> findAllLinesByIds(Set<Integer> lineIds) {
        return productLineJpaRepo.findAllLinesByIds(lineIds);
    }

    @Override
    public List<SpecificationRespondDto> getAllSpecsByItemId(Integer catalogItemId) {
        return specificationJpaRepo.findSpecsByItemId(catalogItemId);
    }

    @Override
    public Map<Integer, String> findAllCatesByIds(Set<Integer> categoryIds) {
        return itemCategoryJpaRepo.findCateByIds(categoryIds);
    }

    @Override
    public int findCategoryMaxOrder() {
        return itemCategoryJpaRepo.findMaxDisplayOrder();
    }
}
