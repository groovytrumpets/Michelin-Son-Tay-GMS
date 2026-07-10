package com.g42.platform.gms.warehouse.app.service.catalog;

import com.g42.platform.gms.estimation.api.internal.TaxRuleInternalApi;
import com.g42.platform.gms.estimation.api.mapper.TaxRuleDtoMapper;
import com.g42.platform.gms.warehouse.api.dto.*;
import com.g42.platform.gms.warehouse.api.mapper.*;
import com.g42.platform.gms.warehouse.app.service.dto.PricingResolve;
import com.g42.platform.gms.warehouse.app.service.pricing.PricingService;
import com.g42.platform.gms.warehouse.domain.entity.*;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.g42.platform.gms.warehouse.infrastructure.repository.InventoryJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryItemJpaRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehousePricingRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.infrastructure.repository.*;
import com.g42.platform.gms.warehouse.infrastructure.entity.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service("catalogItemWarehouseService")
public class CatalogItemService {
    @Autowired
    private InventoryJpaRepo inventoryJpaRepo;
    @Autowired
    private StockEntryItemJpaRepo stockEntryItemJpaRepo;
    @Autowired
    private WarehousePricingRepo warehousePricingRepo;
    @Autowired
    private CatalogItemJpaRepo catalogItemJpaRepo;
    @Autowired
    private BrandJpaRepo brandJpaRepo;
    @Autowired
    private ProductLineJpaRepo productLineJpaRepo;
    @Autowired
    private WorkCategoryJpaEntityRepo itemCategoryJpaRepo;
    @Autowired
    private ProductUnitJpaRepo productUnitJpaRepo;

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
        
        CatalogItem domain = catalogDtoMapper.toDomain(createDto);
        if (domain.getIsActive() == null) {
            domain.setIsActive(true);
        }
        
        CatalogItem catalogItem = catalogItemRepo.createCatalog(domain);
        List<Specification> specifications = catalogItemRepo.getListOfSpecsByItem(catalogItem.getItemId());
        String itemName = builDisplayName(domain,brand,productLine,specifications,itemCategory);
        catalogItem.setItemName(itemName);
        //todo: free tax
        Integer finalTaxId = createDto.getTaxRuleId();
        if (finalTaxId == null) {
            finalTaxId = taxRuleInternalApi.getTaxCodeFreeId("FREE");
            if (finalTaxId==-1) finalTaxId=taxRuleInternalApi.createNewFreeTax();
        }
        catalogItem.setTaxRuleId(finalTaxId);
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
        if (catalogItem.getBrandId() != null && catalogItem.getBrandId() != 0) {
            System.out.println(catalogItem.getBrandId()+" DEBUG");
        catalogDetailDto.setBrandId(catalogItemRepo.getBrandById(catalogItem.getBrandId()).getBrandName());
        }
        if (catalogItem.getProductLineId() != null && catalogItem.getProductLineId() != 0) {
        catalogDetailDto.setProductLine(catalogItemRepo.getProductLineById(catalogItem.getProductLineId()).getLineName());
        }
        if (catalogItem.getTaxRuleId() !=null){
        TaxRuleDto taxValue = taxRuleDtoMapper.toTaxRuleDtoWarehouse
                (taxRuleInternalApi.getTaxRuleById(catalogItem.getTaxRuleId()));
            catalogDetailDto.setTaxRule(taxValue);
        }
        List<WarehouseDetailDto> warehouseDetails = warehouseRepo.getWarehouseDetailsByItemId(catalogDetailDto.getItemId());
        PricingResolve pricingResolve = new PricingResolve();
        for (WarehouseDetailDto warehouseDetailDto : warehouseDetails) {
            pricingResolve = pricingService.getEffectivePrice(catalogItem.getItemId(),warehouseDetailDto.getWarehouseId(),catalogItem.getPrice());
            BigDecimal selinPrice= pricingResolve.getFinalPrice();
            if (pricingResolve==null) {
                selinPrice = BigDecimal.ZERO;
            }
            warehouseDetailDto.setSellingPrice(selinPrice);
            warehouseDetailDto.setNotify(pricingResolve.getNotify());

            PricingResolve pricingResolveWholesale = pricingService.getEffectivePriceWholesale(catalogItem.getItemId(),warehouseDetailDto.getWarehouseId(),catalogItem.getPrice());
            BigDecimal selinPriceWholesale = pricingResolveWholesale != null ? pricingResolveWholesale.getFinalPrice() : BigDecimal.ZERO;
            warehouseDetailDto.setSellingPriceWholesale(selinPriceWholesale);

            Optional<WarehousePricing> pricingOpt = warehousePricingRepo.findByItemIdAndWarehouseId(catalogItem.getItemId(), warehouseDetailDto.getWarehouseId());
            warehouseDetailDto.setHasCustomPricing(pricingOpt.isPresent());

            List<WarehouseLotDto> lots = stockEntryItemJpaRepo.findWarehouseLots(warehouseDetailDto.getWarehouseId(), catalogItem.getItemId());
            BigDecimal warehouseSellingPrice = warehousePricingRepo.findActiveByWarehouseAndItem(warehouseDetailDto.getWarehouseId(), catalogItem.getItemId())
                    .map(WarehousePricing::getSellingPrice)
                    .orElse(null);
            BigDecimal warehouseSellingPriceWholesale = warehousePricingRepo.findActiveByWarehouseAndItem(warehouseDetailDto.getWarehouseId(), catalogItem.getItemId())
                    .map(WarehousePricing::getSellingPriceWholesale)
                    .orElse(null);

            for (WarehouseLotDto lot : lots) {
                BigDecimal lotSellingPrice;
                if (warehouseSellingPrice != null && warehouseSellingPrice.compareTo(BigDecimal.ZERO) > 0) {
                    lotSellingPrice = warehouseSellingPrice;
                } else if (lot.getImportPrice() != null && lot.getImportPrice().compareTo(BigDecimal.ZERO) > 0) {
                    lotSellingPrice = lot.getImportPrice()
                            .multiply(lot.getMarkupMultiplier() != null ? lot.getMarkupMultiplier() : BigDecimal.ONE)
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                } else {
                    lotSellingPrice = catalogItem.getPrice() != null ? catalogItem.getPrice() : BigDecimal.ZERO;
                }
                lot.setSellingPrice(lotSellingPrice);

                BigDecimal lotSellingPriceWholesale;
                if (warehouseSellingPriceWholesale != null && warehouseSellingPriceWholesale.compareTo(BigDecimal.ZERO) > 0) {
                    lotSellingPriceWholesale = warehouseSellingPriceWholesale;
                } else if (lot.getImportPrice() != null && lot.getImportPrice().compareTo(BigDecimal.ZERO) > 0) {
                    lotSellingPriceWholesale = lot.getImportPrice()
                            .multiply(lot.getMarkupMultiplierWholesale() != null ? lot.getMarkupMultiplierWholesale() : lot.getMarkupMultiplier() != null ? lot.getMarkupMultiplier() : BigDecimal.ONE)
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                } else {
                    lotSellingPriceWholesale = catalogItem.getPrice() != null ? catalogItem.getPrice() : BigDecimal.ZERO;
                }
                lot.setSellingPriceWholesale(lotSellingPriceWholesale);
            }
            warehouseDetailDto.setLots(lots);
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

    @Transactional
    public CatalogItemDto updateCatalog(CatalogCreateDto updateDto, Integer itemId) {
        CatalogItem catalogItem = catalogItemRepo.getCatalogItemById(itemId);
        if (catalogItem == null) {
            throw new WarehouseException("Catalog item not found", WarehouseErrorCode.CATALOG_404);
        }

        // Validate SKU only if it changed
        if (updateDto.getSku() != null && !updateDto.getSku().equals(catalogItem.getSku())) {
            if (catalogItemRepo.exitBySku(updateDto.getSku())){
                throw new WarehouseException("Sku is duplicated! please create new sku",
                        WarehouseErrorCode.DUPLICATE_SKU);
            }
        }

        catalogItem.setSku(updateDto.getSku());
        catalogItem.setPrice(updateDto.getPrice());
        catalogItem.setShowPrice(updateDto.getShowPrice());
        catalogItem.setUnit(updateDto.getUnit());
        catalogItem.setWarrantyDurationMonths(updateDto.getWarrantyDurationMonths());
        catalogItem.setBrandId(updateDto.getBrandId());
        catalogItem.setProductLineId(updateDto.getProductLineId());
        catalogItem.setWorkCategoryId(updateDto.getWorkCategoryId());
        if (updateDto.getIsActive() != null) {
            catalogItem.setIsActive(updateDto.getIsActive());
        }
        if (updateDto.getServiceServiceId() != null) {
            catalogItem.setServiceServiceId(updateDto.getServiceServiceId());
        }
        if (updateDto.getDescription() != null) {
            catalogItem.setDescription(updateDto.getDescription());
        }
        if (updateDto.getColor() != null) {
            catalogItem.setColor(updateDto.getColor());
        }
        if (updateDto.getCompatibleCars() != null) {
            catalogItem.setCompatibleCars(updateDto.getCompatibleCars());
        }
        if (updateDto.getMadeIn() != null) {
            catalogItem.setMadeIn(updateDto.getMadeIn());
        } else if (updateDto.getOrigin() != null) {
            catalogItem.setMadeIn(updateDto.getOrigin());
        }

        if (updateDto.getItemName() != null && !updateDto.getItemName().isBlank()) {
            catalogItem.setItemName(updateDto.getItemName());
        } else {
            Brand brand = updateDto.getBrandId() != null ? catalogItemRepo.getBrandById(updateDto.getBrandId()) : null;
            ProductLine productLine = updateDto.getProductLineId() != null ? catalogItemRepo.getProductLineById(updateDto.getProductLineId()) : null;
            WorkCategory itemCategory = updateDto.getWorkCategoryId() != null ? catalogItemRepo.getItemCategoryById(updateDto.getWorkCategoryId()) : null;
            if (itemCategory == null) {
                itemCategory = new WorkCategory();
            }

            List<Specification> specifications = catalogItemRepo.getListOfSpecsByItem(itemId);
            String displayName = builDisplayName(catalogItem, brand, productLine, specifications, itemCategory);
            catalogItem.setItemName(displayName);
        }

        Integer finalTaxId = updateDto.getTaxRuleId();
        if (finalTaxId == null) {
            finalTaxId = taxRuleInternalApi.getTaxCodeFreeId("FREE");
            if (finalTaxId == null || finalTaxId == -1) finalTaxId = taxRuleInternalApi.createNewFreeTax();
        }
        catalogItem.setTaxRuleId(finalTaxId);

        // Process warehouse and lot details
        if (updateDto.getWarehouseDetails() != null) {
            for (WarehouseUpdateDto whDto : updateDto.getWarehouseDetails()) {
                // Update Inventory
                Optional<InventoryJpa> invOpt = inventoryJpaRepo.findByWarehouseIdAndItemId(whDto.getWarehouseId(), itemId);
                InventoryJpa inventory;
                if (invOpt.isPresent()) {
                    inventory = invOpt.get();
                } else {
                    inventory = new InventoryJpa();
                    inventory.setItemId(itemId);
                    inventory.setWarehouseId(whDto.getWarehouseId());
                }
                if (whDto.getQuantity() != null) {
                    inventory.setQuantity(whDto.getQuantity());
                }
                if (whDto.getReservedQuantity() != null) {
                    inventory.setReservedQuantity(whDto.getReservedQuantity());
                }
                inventoryJpaRepo.save(inventory);

                // Update Warehouse Pricing
                if (whDto.getSellingPrice() != null) {
                    Optional<WarehousePricing> pricingOpt = warehousePricingRepo.findByItemIdAndWarehouseId(itemId, whDto.getWarehouseId());
                    if (pricingOpt.isPresent()) {
                        WarehousePricing pricing = pricingOpt.get();
                        pricing.setSellingPrice(whDto.getSellingPrice());
                        pricing.setIsActive(true);
                        warehousePricingRepo.save(pricing);
                    }
                }

                // Update Lots
                if (whDto.getLots() != null) {
                    for (LotUpdateDto lotDto : whDto.getLots()) {
                        if (lotDto.getEntryItemId() != null) {
                            Optional<StockEntryItemJpa> lotOpt = stockEntryItemJpaRepo.findById(lotDto.getEntryItemId());
                            if (lotOpt.isPresent()) {
                                StockEntryItemJpa lotJpa = lotOpt.get();
                                if (lotDto.getRemainingQuantity() != null) {
                                    lotJpa.setRemainingQuantity(lotDto.getRemainingQuantity());
                                }
                                if (lotDto.getImportPrice() != null) {
                                    lotJpa.setImportPrice(lotDto.getImportPrice());
                                }
                                if (lotDto.getMarkupMultiplier() != null) {
                                    lotJpa.setMarkupMultiplier(lotDto.getMarkupMultiplier());
                                } else if (lotDto.getSellingPrice() != null && lotJpa.getImportPrice() != null && lotJpa.getImportPrice().compareTo(BigDecimal.ZERO) > 0) {
                                    BigDecimal multiplier = lotDto.getSellingPrice().divide(lotJpa.getImportPrice(), 4, java.math.RoundingMode.HALF_UP);
                                    lotJpa.setMarkupMultiplier(multiplier);
                                }

                                if (lotDto.getMarkupMultiplierWholesale() != null) {
                                    lotJpa.setMarkupMultiplierWholesale(lotDto.getMarkupMultiplierWholesale());
                                } else if (lotDto.getSellingPriceWholesale() != null && lotJpa.getImportPrice() != null && lotJpa.getImportPrice().compareTo(BigDecimal.ZERO) > 0) {
                                    BigDecimal multiplierWholesale = lotDto.getSellingPriceWholesale().divide(lotJpa.getImportPrice(), 4, java.math.RoundingMode.HALF_UP);
                                    lotJpa.setMarkupMultiplierWholesale(multiplierWholesale);
                                }
                                stockEntryItemJpaRepo.save(lotJpa);
                            }
                        }
                    }
                }
            }
        }

        CatalogItem saved = catalogItemRepo.saveCatalogItem(catalogItem);
        return catalogDtoMapper.toDto(saved);
    }

    @Transactional
    public void deleteBrand(Integer brandId) {
        boolean hasProductLines = productLineJpaRepo.existsByBrandId(brandId);
        boolean hasCatalogItems = catalogItemJpaRepo.existsByBrandId(brandId);
        if (hasProductLines || hasCatalogItems) {
            BrandJpa brandJpa = brandJpaRepo.findById(brandId)
                    .orElseThrow(() -> new WarehouseException("Brand not found", WarehouseErrorCode.INVALID_BRAND));
            brandJpa.setIsActive((byte) 0);
            brandJpaRepo.save(brandJpa);
        } else {
            brandJpaRepo.deleteById(brandId);
        }
    }

    @Transactional
    public void deleteProductLine(Integer productLineId) {
        boolean hasCatalogItems = catalogItemJpaRepo.existsByProductLineId(productLineId);
        if (hasCatalogItems) {
            ProductLineJpa productLineJpa = productLineJpaRepo.findById(productLineId)
                    .orElseThrow(() -> new WarehouseException("Product line not found", WarehouseErrorCode.INVALID_PRODUCT_LINE));
            productLineJpa.setIsActive((byte) 0);
            productLineJpaRepo.save(productLineJpa);
        } else {
            productLineJpaRepo.deleteById(productLineId);
        }
    }

    @Transactional
    public void deleteItemCategory(Integer categoryId) {
        boolean hasCatalogItems = catalogItemJpaRepo.existsByWorkCategoryId(categoryId);
        if (hasCatalogItems) {
            WorkCategoryJpaEntity workCategory = itemCategoryJpaRepo.findById(categoryId)
                    .orElseThrow(() -> new WarehouseException("Category not found", WarehouseErrorCode.INVALID_CATEGORY));
            workCategory.setIsActive(false);
            itemCategoryJpaRepo.save(workCategory);
        } else {
            itemCategoryJpaRepo.deleteById(categoryId);
        }
    }

    public List<ProductUnitJpa> getAllProductUnits() {
        return productUnitJpaRepo.findAll();
    }

    @Transactional
    public ProductUnitJpa createProductUnit(String unitName) {
        String trimmed = unitName != null ? unitName.trim() : "";
        if (trimmed.isEmpty()) {
            throw new WarehouseException("Unit name cannot be empty", WarehouseErrorCode.INVALID_CATEGORY);
        }
        if (productUnitJpaRepo.existsByUnitName(trimmed)) {
            ProductUnitJpa existing = productUnitJpaRepo.findByUnitName(trimmed);
            if (existing.getIsActive() == 0) {
                existing.setIsActive((byte) 1);
                return productUnitJpaRepo.save(existing);
            }
            throw new WarehouseException("Unit name already exists", WarehouseErrorCode.INVALID_CATEGORY);
        }
        ProductUnitJpa unit = new ProductUnitJpa();
        unit.setUnitName(trimmed);
        unit.setIsActive((byte) 1);
        return productUnitJpaRepo.save(unit);
    }

    @Transactional
    public void deleteProductUnit(Integer unitId) {
        ProductUnitJpa unit = productUnitJpaRepo.findById(unitId)
                .orElseThrow(() -> new WarehouseException("Unit not found", WarehouseErrorCode.INVALID_CATEGORY));
        boolean isUsed = catalogItemJpaRepo.existsByUnit(unit.getUnitName());
        if (isUsed) {
            unit.setIsActive((byte) 0);
            productUnitJpaRepo.save(unit);
        } else {
            productUnitJpaRepo.deleteById(unitId);
        }
    }
}

