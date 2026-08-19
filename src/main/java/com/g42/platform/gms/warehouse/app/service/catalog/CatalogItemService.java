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
import com.g42.platform.gms.vehicle.entity.VehicleBrand;
import com.g42.platform.gms.vehicle.entity.VehicleModel;
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
    private ItemCategoryJpaRepo itemCategoryJpaRepo;
    @Autowired
    private ProductUnitJpaRepo productUnitJpaRepo;
    @Autowired
    private CatalogItemCompatJpaRepo catalogItemCompatJpaRepo;
    @Autowired
    private com.g42.platform.gms.vehicle.repository.VehicleBrandRepository vehicleBrandRepository;
    @Autowired
    private com.g42.platform.gms.vehicle.repository.VehicleModelRepository vehicleModelRepository;

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
    private ItemCategoryDtoMapper itemCateDtoMapper;
    @Autowired
    private ItemCategoryService itemCategoryService;
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
    @Autowired
    private ItemColorDtoMapper itemColorDtoMapper;

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
        ItemCategory itemCategory = catalogItemRepo.getItemCategoryById(createDto.getItemCategoryId());
        
        CatalogItem domain = catalogDtoMapper.toDomain(createDto);
        if (domain.getIsActive() == null) {
            domain.setIsActive(true);
        }
        
        CatalogItem catalogItem = catalogItemRepo.createCatalog(domain);
        List<Specification> specifications = catalogItemRepo.getListOfSpecsByItem(catalogItem.getItemId());
        // Respect a manually entered item name; only auto-suggest one when the caller left it blank.
        String itemName = (createDto.getItemName() != null && !createDto.getItemName().isBlank())
                ? createDto.getItemName().trim()
                : builDisplayName(domain,brand,productLine,specifications,itemCategory);
        catalogItem.setItemName(itemName);
        catalogItem.setSearchKey(buildSearchKey(catalogItem, brand, productLine, specifications, itemCategory));
        //todo: free tax
        Integer finalTaxId = createDto.getTaxRuleId();
        if (finalTaxId == null) {
            finalTaxId = taxRuleInternalApi.getTaxCodeFreeId("FREE");
            if (finalTaxId==-1) finalTaxId=taxRuleInternalApi.createNewFreeTax();
        }
        catalogItem.setTaxRuleId(finalTaxId);
        CatalogItem saveCatalogItem = catalogItemRepo.saveCatalogItem(catalogItem);
        replaceCompatibilities(saveCatalogItem.getItemId(), createDto.getCompatibilities());
        return catalogDtoMapper.toDto(saveCatalogItem);
    }

    /**
     * Ghi đè toàn bộ danh sách xe tương thích của một vật tư.
     * Truyền null nghĩa là không đụng tới danh sách hiện có; truyền list rỗng là xóa hết.
     */
    private void replaceCompatibilities(Integer itemId, List<CatalogItemCompatDto> compatibilities) {
        if (itemId == null || compatibilities == null) return;

        catalogItemCompatJpaRepo.deleteByItemId(itemId);
        if (compatibilities.isEmpty()) return;

        List<CatalogItemCompatJpa> rows = new ArrayList<>();
        for (CatalogItemCompatDto dto : compatibilities) {
            if (dto == null) continue;
            // Dòng trống hoàn toàn thì bỏ qua, tránh rác trong bảng
            if (dto.getBrandId() == null && dto.getModelId() == null
                    && dto.getYearFrom() == null && dto.getYearTo() == null) {
                continue;
            }
            CatalogItemCompatJpa row = new CatalogItemCompatJpa();
            row.setItemId(itemId);
            row.setBrandId(dto.getBrandId());
            row.setModelId(dto.getModelId());
            row.setYearFrom(dto.getYearFrom());
            row.setYearTo(dto.getYearTo());
            rows.add(row);
        }
        if (!rows.isEmpty()) {
            catalogItemCompatJpaRepo.saveAll(rows);
        }
    }

    /** Đọc danh sách xe tương thích kèm tên hãng / dòng để hiển thị. */
    private List<CatalogItemCompatDto> loadCompatibilities(Integer itemId) {
        if (itemId == null) return List.of();
        List<CatalogItemCompatJpa> rows = catalogItemCompatJpaRepo.findByItemId(itemId);
        if (rows.isEmpty()) return List.of();

        // Gom id rồi tra một lượt, tránh truy vấn lặp theo từng dòng
        List<Integer> brandIds = rows.stream().map(CatalogItemCompatJpa::getBrandId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        List<Integer> modelIds = rows.stream().map(CatalogItemCompatJpa::getModelId)
                .filter(java.util.Objects::nonNull).distinct().toList();

        java.util.Map<Integer, String> brandNames = brandIds.isEmpty() ? java.util.Map.of()
                : vehicleBrandRepository.findAllById(brandIds).stream()
                        .collect(java.util.stream.Collectors.toMap(VehicleBrand::getBrandId, VehicleBrand::getName));
        java.util.Map<Integer, String> modelNames = modelIds.isEmpty() ? java.util.Map.of()
                : vehicleModelRepository.findAllById(modelIds).stream()
                        .collect(java.util.stream.Collectors.toMap(VehicleModel::getModelId, VehicleModel::getName));

        return rows.stream()
                .map(row -> new CatalogItemCompatDto(
                        row.getCompatId(),
                        row.getBrandId(),
                        row.getBrandId() == null ? null : brandNames.get(row.getBrandId()),
                        row.getModelId(),
                        row.getModelId() == null ? null : modelNames.get(row.getModelId()),
                        row.getYearFrom(),
                        row.getYearTo()))
                .toList();
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
    private String builDisplayName(CatalogItem catalogItem, Brand brand, ProductLine productLine, List<Specification> specs,ItemCategory itemCategory) {
        StringBuilder displayName = new StringBuilder();

        // Danh mục là tùy chọn, tên gợi ý bỏ qua phần này khi hàng chưa xếp danh mục
        if (itemCategory != null && itemCategory.getCategoryName() != null && !itemCategory.getCategoryName().isBlank()) {
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

    /**
     * Builds a denormalized lowercase blob of every field a user might search by
     * (name, codes, brand/category/product line names, spec values, compatible cars),
     * so search can match a single column instead of joining/LIKE-ing many.
     */
    private String buildSearchKey(CatalogItem catalogItem, Brand brand, ProductLine productLine, List<Specification> specs, ItemCategory itemCategory) {
        StringBuilder searchKey = new StringBuilder();
        appendSearchToken(searchKey, catalogItem.getItemName());
        appendSearchToken(searchKey, catalogItem.getSku());
        appendSearchToken(searchKey, catalogItem.getPartNumber());
        appendSearchToken(searchKey, catalogItem.getBarcode());
        appendSearchToken(searchKey, catalogItem.getColor());
        appendSearchToken(searchKey, catalogItem.getMadeIn());
        appendSearchToken(searchKey, catalogItem.getCompatibleCars());
        appendSearchToken(searchKey, catalogItem.getDescription());
        if (brand != null) {
            appendSearchToken(searchKey, brand.getBrandName());
        }
        if (productLine != null) {
            appendSearchToken(searchKey, productLine.getLineName());
        }
        if (itemCategory != null) {
            appendSearchToken(searchKey, itemCategory.getCategoryName());
        }
        if (specs != null) {
            for (Specification specification : specs) {
                appendSearchToken(searchKey, specification.getSpecValue());
            }
        }
        String result = searchKey.toString().trim().toLowerCase();
        return result.length() > 2000 ? result.substring(0, 2000) : result;
    }

    private void appendSearchToken(StringBuilder searchKey, String value) {
        if (value != null && !value.isBlank()) {
            searchKey.append(value.trim()).append(" ");
        }
    }

    public ProductLine saveProductLine(ProductLine productLine) {
        if (productLine.getBrandId() == null) {
            throw new WarehouseException("Product line must have brand", WarehouseErrorCode.INVALID_BRAND);
        }
        return catalogItemRepo.saveProductLine(productLine);
    }
    /**
     * Tạo danh mục từ các màn kho cũ. Loại danh mục (PART/SERVICE) và thuế nay đều
     * không bắt buộc — hàng hóa được phép không thuộc danh mục nào, nên chặn ở đây
     * chỉ tổ làm người dùng không tạo nổi danh mục.
     */
    @Transactional
    public ItemCategoryDto saveItemCate(ItemCategory itemCategory) {
        return itemCategoryService.create(itemCategory);
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
        ItemCategory itemCategory = catalogItemRepo.getItemCategoryById(catalogItem.getItemCategoryId());
        Specification savedSpec = catalogItemRepo.saveSpec(specification);
        List<Specification> specifications = catalogItemRepo.getListOfSpecsByItem(catalogItem.getItemId());
        // Only auto-fill the item name when none was ever set (e.g. left blank at creation);
        // a manually entered or already-suggested name must not be silently overwritten
        // just because a spec value changed.
        if (catalogItem.getItemName() == null || catalogItem.getItemName().isBlank()) {
            catalogItem.setItemName(builDisplayName(catalogItem,brand,productLine,specifications,itemCategory));
        }
        catalogItem.setSearchKey(buildSearchKey(catalogItem, brand, productLine, specifications, itemCategory));
        CatalogItem saveCatalogItem = catalogItemRepo.saveCatalogItem(catalogItem);

        return savedSpec;
    }

    public SpecAttribute saveSpecAttribute(SpecAttribute specAttribute) {
        return catalogItemRepo.saveSpecAttribute(specAttribute);
    }

    public List<ItemColorDto> getColorsByItemId(Integer itemId) {
        return catalogItemRepo.getColorsByItemId(itemId).stream().map(itemColorDtoMapper::toDto).toList();
    }

    /** Chi tiết an toàn cho trang bán hàng công khai — không lộ costPrice/tồn kho nội bộ. */
    public PublicPartDetailDto getPublicPartDetail(Integer catalogItemId) {
        CatalogItem catalogItem = catalogItemRepo.getCatalogItemById(catalogItemId);
        if (catalogItem == null) {
            return null;
        }
        String productLineName = null;
        if (catalogItem.getProductLineId() != null && catalogItem.getProductLineId() != 0) {
            ProductLine productLine = catalogItemRepo.getProductLineById(catalogItem.getProductLineId());
            productLineName = productLine != null ? productLine.getLineName() : null;
        }
        PublicPartDetailDto dto = new PublicPartDetailDto();
        dto.setOrigin(catalogItem.getMadeIn());
        dto.setUnit(catalogItem.getUnit());
        dto.setProductLine(productLineName);
        dto.setColors(getColorsByItemId(catalogItemId));
        dto.setSpecifications(catalogItemRepo.getAllSpecsByItemId(catalogItemId));
        return dto;
    }

    @Transactional
    public List<ItemColorDto> replaceItemColors(Integer itemId, List<ItemColorDto> colors) {
        if (itemId == null) {
            throw new WarehouseException("item Catalog required!", WarehouseErrorCode.PARENT_REQUIRE);
        }
        List<ItemColor> domainColors = colors == null ? List.of()
                : colors.stream().map(itemColorDtoMapper::toDomain).toList();
        List<ItemColor> saved = catalogItemRepo.replaceItemColors(itemId, domainColors);
        return saved.stream().map(itemColorDtoMapper::toDto).toList();
    }

    public List<ItemCategoryDto> getAllItemCategory() {
        List<ItemCategory> itemCategories = catalogItemRepo.getAllItemCategory();
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
        catalogDetailDto.setCompatibilities(loadCompatibilities(catalogItemId));
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
        catalogItem.setItemCategoryId(updateDto.getItemCategoryId());
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
        if (updateDto.getTechnicalSpecs() != null) {
            catalogItem.setTechnicalSpecs(updateDto.getTechnicalSpecs());
        }
        if (updateDto.getUserGuide() != null) {
            catalogItem.setUserGuide(updateDto.getUserGuide());
        }
        if (updateDto.getDealerWarrantyMonths() != null) {
            catalogItem.setDealerWarrantyMonths(updateDto.getDealerWarrantyMonths());
        }
        if (updateDto.getCostPrice() != null) {
            catalogItem.setCostPrice(updateDto.getCostPrice());
        }
        replaceCompatibilities(itemId, updateDto.getCompatibilities());

        // Brand/productLine/itemCategory ids on catalogItem are already the final (updated) values at this point.
        Brand brandForSearch = catalogItem.getBrandId() != null ? catalogItemRepo.getBrandById(catalogItem.getBrandId()) : null;
        ProductLine productLineForSearch = catalogItem.getProductLineId() != null ? catalogItemRepo.getProductLineById(catalogItem.getProductLineId()) : null;
        ItemCategory itemCategoryForSearch = catalogItem.getItemCategoryId() != null ? catalogItemRepo.getItemCategoryById(catalogItem.getItemCategoryId()) : null;
        List<Specification> specificationsForSearch = catalogItemRepo.getListOfSpecsByItem(itemId);

        if (updateDto.getItemName() != null && !updateDto.getItemName().isBlank()) {
            catalogItem.setItemName(updateDto.getItemName());
        } else {
            ItemCategory categoryForName = itemCategoryForSearch != null ? itemCategoryForSearch : new ItemCategory();
            String displayName = builDisplayName(catalogItem, brandForSearch, productLineForSearch, specificationsForSearch, categoryForName);
            catalogItem.setItemName(displayName);
        }
        catalogItem.setSearchKey(buildSearchKey(catalogItem, brandForSearch, productLineForSearch, specificationsForSearch, itemCategoryForSearch));

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
        boolean hasCatalogItems = catalogItemJpaRepo.existsByItemCategoryId(categoryId);
        if (hasCatalogItems) {
            ItemCategoryJpa itemCategory = itemCategoryJpaRepo.findById(categoryId)
                    .orElseThrow(() -> new WarehouseException("Category not found", WarehouseErrorCode.INVALID_CATEGORY));
            itemCategory.setIsActive(false);
            itemCategoryJpaRepo.save(itemCategory);
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

