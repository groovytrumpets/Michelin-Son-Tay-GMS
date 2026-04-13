package com.g42.platform.gms.warehouse.app.service.catalog;

import com.g42.platform.gms.estimation.api.internal.TaxRuleInternalApi;
import com.g42.platform.gms.estimation.api.mapper.TaxRuleDtoMapper;
import com.g42.platform.gms.warehouse.api.dto.*;
import com.g42.platform.gms.warehouse.api.mapper.*;
import com.g42.platform.gms.warehouse.app.service.pricing.PricingService;
import com.g42.platform.gms.warehouse.domain.entity.*;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service("catalogItemWarehouseService")
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
    @Autowired
    private CatalogDtoMapper catalogDtoMapper;
    @Autowired
    private WorkCateDtoMapper itemCateDtoMapper;
    @Autowired
    private TaxRuleInternalApi taxRuleInternalApi;
    @Autowired
    private TaxRuleDtoMapper taxRuleDtoMapper;
    @Autowired
    private WarehouseRepo warehouseRepo;
    @Autowired
    private PricingService pricingService;
    @Autowired
    private WarehouseDtoMapper warehouseDtoMapper;

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

    public Brand createNewBrand(Brand brand) {
        return catalogItemRepo.createBrand(brand);
    }
    @Transactional
    public CatalogItemDto createNewCatalog(CatalogCreateDto createDto) {
        validateCatalogItemDto(createDto);
        Brand brand = catalogItemRepo.getBrandById(createDto.getBrandId());
        ProductLine productLine = catalogItemRepo.getProductLineById(createDto.getProductLineId());
        WorkCategory itemCategory = catalogItemRepo.getItemCategoryById(createDto.getWorkCategoryId());
        CatalogItem catalogItem = catalogItemRepo.createCatalog(catalogDtoMapper.toDomain(createDto));
        List<Specification> specifications = catalogItemRepo.getListOfSpecsByItem(catalogItem.getItemId());
        String itemName = builDisplayName(catalogDtoMapper.toDomain(createDto),brand,productLine,specifications,itemCategory);
        catalogItem.setItemName(itemName);
        CatalogItem saveCatalogItem = catalogItemRepo.saveCatalogItem(catalogItem);
        return catalogDtoMapper.toDto(saveCatalogItem);
    }

    private void validateCatalogItemDto(CatalogCreateDto createDto) {
        if (createDto.getBrandId() != null) {
            Brand brand = catalogItemRepo.getBrandById(createDto.getBrandId());
            if (brand == null||brand.getIsActive().equals((byte)0)) {
                throw new WarehouseException("Brand suggetion is unavailable! please create new brand",
                        WarehouseErrorCode.INVALID_BRAND);
            }
        }
        if (catalogItemRepo.exitBySku(createDto.getSku())){
            throw new WarehouseException("Sku is duplicated! please create new sku",
                    WarehouseErrorCode.DUPLICATE_SKU);
        }
        //todo:validate catalog category items
    }
    private String builDisplayName(CatalogItem catalogItem, Brand brand, ProductLine productLine, List<Specification> specs,WorkCategory itemCategory) {
        StringBuilder displayName = new StringBuilder();

        //type
        if (itemCategory.getCategoryName() != null && !itemCategory.getCategoryName().isBlank()) {
            displayName.append(itemCategory.getCategoryName()).append(" ");
        }

        if (brand!=null && brand.getBrandName() != null && !brand.getBrandName().isBlank()) {
            displayName.append(brand.getBrandName()).append(" ");
        }

        if (specs !=null && !specs.isEmpty()) {
            //todo: buildSpecString (advance)
//            String specStr = buildSpecString(specs);
//            if (specStr != null && !specStr.isBlank()) {
//                displayName.append(specStr).append(" ");
//            }
            for (Specification specification : specs) {
                displayName.append(specification.getSpecValue());
                SpecAttribute specAttribute = catalogItemRepo.getSpecAttributeById(specification.getAttributeId());
                displayName.append(specAttribute.getUnit()).append(" ");
            }
        }

        if (productLine != null) {
            displayName.append(productLine.getLineName()).append(" ");
        }

        if (displayName.isEmpty() && itemCategory.getCategoryName() != null && !itemCategory.getCategoryName().isBlank()) {
            displayName.append(itemCategory.getCategoryName());
        }
        return displayName.toString().trim();

    }

    public ProductLine saveProductLine(ProductLine productLine) {
        if (productLine.getBrandId() == null) {
            throw new WarehouseException("Product line must have brand", WarehouseErrorCode.INVALID_BRAND);
        }
        return catalogItemRepo.saveProductLine(productLine);
    }
    @Transactional
    public WorkCategory saveItemCate(WorkCategory itemCategory) {
        if (itemCategory.getCategoryType()==null) {
            throw new WarehouseException("Category must not null!",WarehouseErrorCode.WRONG_ENUM);
        }
        if (!itemCategory.getCategoryType().equals("SERVICE") && !itemCategory.getCategoryType().equals("PART")) {
            throw new WarehouseException("Category type must be PART or SERVICE!",WarehouseErrorCode.WRONG_ENUM);
        }
        if (catalogItemRepo.exitByCategoryCode(itemCategory.getCategoryCode()))
            throw new WarehouseException("Category code must be UNIQUE!",WarehouseErrorCode.INVALID_CATEGORY);
        itemCategory.setIsActive(true);
        int nextOrder = catalogItemRepo.findCategoryMaxOrder()+1;
        itemCategory.setDisplayOrder(nextOrder);

        Integer finalTaxId = itemCategory.getTaxRuleId();
        if (finalTaxId == null){
            finalTaxId = taxRuleInternalApi.getTaxCodeFreeId("FREE");
            if (finalTaxId==null) {
                finalTaxId = taxRuleInternalApi.createNewFreeTax();

            }
        }
        itemCategory.setTaxRuleId(finalTaxId);
        return catalogItemRepo.saveItemCate(itemCategory);
    }
    @Transactional
    public Specification saveSpecs(Specification specification) {
        if (specification.getItemId()==null) {
            throw new WarehouseException("item Catalog required!",WarehouseErrorCode.PARENT_REQUIRE);
        }
        //todo: update catalogName
        CatalogItem catalogItem = catalogItemRepo.getCatalogItemById(specification.getItemId());
        Brand brand = catalogItemRepo.getBrandById(catalogItem.getBrandId());
        ProductLine productLine = catalogItemRepo.getProductLineById(catalogItem.getProductLineId());
        WorkCategory itemCategory = catalogItemRepo.getItemCategoryById(catalogItem.getWorkCategoryId());
        Specification savedSpec = catalogItemRepo.saveSpec(specification);
        List<Specification> specifications = catalogItemRepo.getListOfSpecsByItem(catalogItem.getItemId());
        String itemName = builDisplayName(catalogItem,brand,productLine,specifications,itemCategory);
        catalogItem.setItemName(itemName);
        CatalogItem saveCatalogItem = catalogItemRepo.saveCatalogItem(catalogItem);

        return savedSpec;
    }

    public SpecAttribute saveSpecAttribute(SpecAttribute specAttribute) {
        return catalogItemRepo.saveSpecAttribute(specAttribute);
    }

    public List<WorkCategoryHintDto> getAllItemCategory() {
        List<WorkCategory> itemCategories = catalogItemRepo.getAllItemCategory();
        return itemCategories.stream().map(itemCateDtoMapper::toDto).toList();
    }

    public List<SpecificationDto> getAllSpecsById(Integer catalogItemId) {
        List<Specification> specifications = catalogItemRepo.getListOfSpecsByItem(catalogItemId);
        return specifications.stream().map(specificationDtoMapper::toDto).toList();
    }
    public Integer findCodeByCategoryCode(String categoryCode) {
        return catalogItemRepo.findCategoryCode(categoryCode);
    }

    public CatalogDetailDto getCatalogDetailById(Integer catalogItemId) {
        CatalogItem catalogItem = catalogItemRepo.getCatalogItemById(catalogItemId);
        CatalogDetailDto catalogDetailDto = catalogDtoMapper.toDetailDto(catalogItem);
        catalogDetailDto.setSpecifications(catalogItemRepo.getAllSpecsByItemId(catalogItemId));
        if (catalogItem.getBrandId() != 0) {
            System.out.println(catalogItem.getBrandId()+" DEBUG");
        catalogDetailDto.setBrandId(catalogItemRepo.getBrandById(catalogItem.getBrandId()).getBrandName());
        }
        if (catalogItem.getProductLineId() != 0) {
        catalogDetailDto.setProductLine(catalogItemRepo.getProductLineById(catalogItem.getProductLineId()).getLineName());
        }
        if (catalogItem.getTaxRuleId() !=null){
        TaxRuleDto taxValue = taxRuleDtoMapper.toTaxRuleDtoWarehouse
                (taxRuleInternalApi.getTaxRuleById(catalogItem.getTaxRuleId()));
            catalogDetailDto.setTaxRule(taxValue);
        }
        List<WarehouseDetailDto> warehouseDetails = warehouseRepo.getWarehouseDetailsByItemId(catalogDetailDto.getItemId());
        for (WarehouseDetailDto warehouseDetailDto : warehouseDetails) {
            BigDecimal selinPrice= pricingService.getEffectivePrice(catalogItem.getItemId(),warehouseDetailDto.getWarehouseId(),catalogItem.getPrice());
            if (selinPrice==null) {
                selinPrice = BigDecimal.ZERO;
            }
            warehouseDetailDto.setSellingPrice(selinPrice);
        }


        catalogDetailDto.setWarehouseDetails(warehouseDetails);

        return catalogDetailDto;
    }

    public SpecAttributeDto getSpecsAttributeById(Integer attributeId) {
        return specAttributeDtoMapper.toDto(catalogItemRepo.getSpecAttributeById(attributeId));
    }

    public List<WarehouseDto> getAllWarehouse() {
        List<Warehouse> warehouses = warehouseRepo.getAllWarehouse();
        return warehouses.stream().map(warehouseDtoMapper::toDto).toList();
    }
}
