package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

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
    private ItemCategoryEntityJpaMapper itemCategoryJpaMapper;
    @Autowired
    private ItemCategoryJpaRepo itemCategoryJpaRepo;
    @Autowired
    private ItemColorJpaRepo itemColorJpaRepo;
    @Autowired
    private ItemColorJpaMapper itemColorJpaMapper;


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
        if (brandId == null) {
            return null;
        }
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
    public boolean existsActiveBySku(String sku, Integer excludeItemId) {
        return catalogItemJpaRepo.existsActiveBySku(sku, excludeItemId);
    }
    @Override
    public boolean existsActiveBySlug(String slug, Integer excludeItemId) {
        return catalogItemJpaRepo.existsActiveBySlug(slug, excludeItemId);
    }
    @Override
    public void releaseSlugFromInactive(String slug) {
        catalogItemJpaRepo.releaseSlugFromInactive(slug);
    }
    @Override
    public Integer findItemIdBySlug(String slug) {
        return catalogItemJpaRepo.findBySlug(slug).map(CatalogItemJpa::getItemId).orElse(null);
    }
    @Override
    public Long findServiceIdBySlug(String slug) {
        return catalogItemJpaRepo.findBySlug(slug).map(CatalogItemJpa::getServiceId).orElse(null);
    }
    @Override
    @Transactional
    public ItemCategory saveItemCate(ItemCategory itemCategory) {

        ItemCategoryJpa itemCategoryJpa = itemCategoryJpaRepo.save(itemCategoryJpaMapper.toJpa(itemCategory));
        return itemCategoryJpaMapper.toDomain(itemCategoryJpa);
    }

    @Override
    public ProductLine getProductLineById(Integer productLineId) {
        if (productLineId == null) {
            return null;
        }
        ProductLineJpa itemJpa = productLineJpaRepo.findById(productLineId).orElse(null);
        return productLineJpaMapper.toDomain(itemJpa);
    }

    @Override
    public List<Specification> getListOfSpecsByItem(Integer itemId) {
        List<SpecificationJpa> specificationJpas = specificationJpaRepo.findAllByItemId(itemId);
        return specificationJpas.stream().map(specificationJpaMapper::toDomain).toList();
    }

    @Override
    public ItemCategory getItemCategoryById(Integer itemCategoryId) {
        // Hàng hóa được phép không có danh mục, nên id null là trường hợp bình thường
        // chứ không phải lỗi — findById(null) sẽ ném ngoại lệ.
        if (itemCategoryId == null) return null;
        ItemCategoryJpa itemCategoryJpa = itemCategoryJpaRepo.findById(itemCategoryId).orElse(null);
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
    public List<ItemCategory> getAllItemCategory() {
        List<ItemCategoryJpa> itemCategoryJpas = itemCategoryJpaRepo.findAll();
        return itemCategoryJpas.stream()
                .filter(c -> c.getIsActive() == null || c.getIsActive())
                .map(itemCategoryJpaMapper::toDomain)
                .toList();
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
        ItemCategoryJpa itemCategoryJpaEntity = itemCategoryJpaRepo.findFirstByCategoryCode(categoryCode);
        if (itemCategoryJpaEntity == null) {
            return null;
        }
        return itemCategoryJpaEntity.getItemCategoryId();
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
    public Map<Integer, String> findAllCateNamesByIds(Set<Integer> categoryIds) {
        return itemCategoryJpaRepo.findCateNamesByIds(categoryIds);
    }

    @Override
    public int findCategoryMaxOrder() {
        return itemCategoryJpaRepo.findMaxDisplayOrder();
    }

    @Override
    public List<ItemColor> getColorsByItemId(Integer itemId) {
        return itemColorJpaRepo.findByItemIdOrderByDisplayOrderAscItemColorIdAsc(itemId)
                .stream().map(itemColorJpaMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public List<ItemColor> replaceItemColors(Integer itemId, List<ItemColor> colors) {
        itemColorJpaRepo.deleteByItemId(itemId);
        List<ItemColorJpa> toSave = colors.stream().map(c -> {
            ItemColorJpa jpa = itemColorJpaMapper.toJpa(c);
            jpa.setItemColorId(null);
            jpa.setItemId(itemId);
            return jpa;
        }).toList();
        List<ItemColorJpa> saved = itemColorJpaRepo.saveAll(toSave);
        return saved.stream().map(itemColorJpaMapper::toDomain).toList();
    }
}
