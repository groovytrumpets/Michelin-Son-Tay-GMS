package com.g42.platform.gms.warehouse.app.service.catalog;


import com.g42.platform.gms.common.util.Qty;
import com.g42.platform.gms.estimation.api.internal.TaxRuleInternalApi;
import com.g42.platform.gms.estimation.api.mapper.TaxRuleDtoMapper;
import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.repository.ServiceJpaRepository;
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
import java.time.LocalDateTime;
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
    private StockEntryJpaRepo stockEntryJpaRepo;
    @Autowired
    private InventoryTransactionJpaRepo inventoryTransactionJpaRepo;
    @Autowired
    private WarehousePricingRepo warehousePricingRepo;
    @Autowired
    private CatalogItemJpaRepo catalogItemJpaRepo;
    @Autowired
    private ServiceJpaRepository serviceJpaRepository;
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
    @Autowired
    private ItemQuantityPolicy itemQuantityPolicy;
    @Autowired
    private ItemSerialJpaRepo itemSerialJpaRepo;

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
        applyMeasurementConfig(domain, createDto, null);
        String normalizedSlug = normalizeSlug(createDto.getSlug());
        if (normalizedSlug != null) {
            if (catalogItemRepo.existsActiveBySlug(normalizedSlug, null)) {
                throw new WarehouseException("Đường dẫn đã được dùng cho mặt hàng khác, hãy chọn đường dẫn khác",
                        WarehouseErrorCode.DUPLICATE_SLUG);
            }
            catalogItemRepo.releaseSlugFromInactive(normalizedSlug);
        }
        domain.setSlug(normalizedSlug);

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
            // is_active để trống là hãng cũ nhập trước khi có cột này — coi như đang dùng.
            // Gọi thẳng brand.getIsActive().equals(...) sẽ ném NPE và biến thành lỗi 500.
            if (brand == null || Byte.valueOf((byte) 0).equals(brand.getIsActive())) {
                throw new WarehouseException("Brand suggetion is unavailable! please create new brand",
                        WarehouseErrorCode.INVALID_BRAND);
            }
        }
        if (catalogItemRepo.existsActiveBySku(createDto.getSku(), null)){
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

        // itemCategory có thể null (hàng chưa xếp danh mục), phải kiểm tra lại ở đây
        if (displayName.isEmpty() && itemCategory != null
                && itemCategory.getCategoryName() != null && !itemCategory.getCategoryName().isBlank()) {
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

    /**
     * Chuẩn hoá đường dẫn chữ tuỳ chỉnh: chữ thường, bỏ dấu, chỉ còn a-z0-9 và gạch
     * ngang. Chuỗi rỗng/blank trở thành null (không có slug) — không lưu chuỗi rỗng
     * vì cột có unique index và MySQL chỉ coi NULL mới không đụng nhau.
     */
    private String normalizeSlug(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        String normalized = java.text.Normalizer.normalize(trimmed, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.length() > 220) normalized = normalized.substring(0, 220);
        return normalized.isEmpty() ? null : normalized;
    }

    public SpecAttribute saveSpecAttribute(SpecAttribute specAttribute) {
        return catalogItemRepo.saveSpecAttribute(specAttribute);
    }

    public List<ItemColorDto> getColorsByItemId(Integer itemId) {
        return catalogItemRepo.getColorsByItemId(itemId).stream().map(itemColorDtoMapper::toDto).toList();
    }

    /** Tra catalogItemId từ đường dẫn chữ tuỳ chỉnh — dùng để mở /parts|/services/{slug}. */
    public Integer findCatalogItemIdBySlug(String slug) {
        String normalized = normalizeSlug(slug);
        return normalized == null ? null : catalogItemRepo.findItemIdBySlug(normalized);
    }

    /**
     * serviceId của mặt hàng theo đường dẫn chữ — để FE mở đúng /home/service/{serviceId}.
     * Trước đây FE chỉ có catalogItemId nên gọi /home/service/{catalogItemId} và bị 404 với
     * phụ tùng (serviceId != catalogItemId). Null nếu slug không tồn tại hoặc mặt hàng chưa
     * gắn với bản ghi service nào.
     */
    public Long findServiceIdBySlug(String slug) {
        String normalized = normalizeSlug(slug);
        return normalized == null ? null : catalogItemRepo.findServiceIdBySlug(normalized);
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
            if (Boolean.TRUE.equals(catalogItem.getTracksSerial()) && !lots.isEmpty()) {
                java.util.Map<Integer, Long> serialCounts = new java.util.HashMap<>();
                for (Object[] row : itemSerialJpaRepo.countByLots(
                        lots.stream().map(WarehouseLotDto::getEntryItemId).toList(),
                        List.of(com.g42.platform.gms.warehouse.domain.enums.SerialStatus.IN_STOCK,
                                com.g42.platform.gms.warehouse.domain.enums.SerialStatus.RESERVED))) {
                    serialCounts.put((Integer) row[0], ((Number) row[1]).longValue());
                }
                lots.forEach(lot -> lot.setSerialCount(serialCounts.getOrDefault(lot.getEntryItemId(), 0L)));
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
    public CatalogItemDto updateCatalog(CatalogCreateDto updateDto, Integer itemId, Integer staffId) {
        CatalogItem catalogItem = catalogItemRepo.getCatalogItemById(itemId);
        if (catalogItem == null) {
            throw new WarehouseException("Catalog item not found", WarehouseErrorCode.CATALOG_404);
        }

        // Validate SKU only if it changed
        if (updateDto.getSku() != null && !updateDto.getSku().equals(catalogItem.getSku())) {
            if (catalogItemRepo.existsActiveBySku(updateDto.getSku(), itemId)){
                throw new WarehouseException("Sku is duplicated! please create new sku",
                        WarehouseErrorCode.DUPLICATE_SKU);
            }
        }

        if (updateDto.getSlug() != null) {
            String normalizedSlug = normalizeSlug(updateDto.getSlug());
            if (normalizedSlug != null && !normalizedSlug.equals(catalogItem.getSlug())) {
                if (catalogItemRepo.existsActiveBySlug(normalizedSlug, itemId)) {
                    throw new WarehouseException("Đường dẫn đã được dùng cho mặt hàng khác, hãy chọn đường dẫn khác",
                            WarehouseErrorCode.DUPLICATE_SLUG);
                }
                catalogItemRepo.releaseSlugFromInactive(normalizedSlug);
            }
            catalogItem.setSlug(normalizedSlug);
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
        applyMeasurementConfig(catalogItem, updateDto, itemId);
        ItemQuantityPolicy.Rules quantityRules = ItemQuantityPolicy.Rules.of(catalogItem);
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
                itemQuantityPolicy.validateScale(quantityRules, whDto.getQuantity(), "Tồn kho");
                itemQuantityPolicy.validateScale(quantityRules, whDto.getReservedQuantity(), "Hàng giữ");
                if (Boolean.TRUE.equals(catalogItem.getTracksSerial())) {
                    guardSerialStockEdit(inventory, whDto);
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
                        itemQuantityPolicy.validateScale(quantityRules, lotDto.getRemainingQuantity(), "Lô " +
                                (lotDto.getEntryCode() != null ? lotDto.getEntryCode() : ""));
                        if (lotDto.getEntryItemId() == null) {
                            // Lô mới người dùng thêm tay ở popup. Trước đây nhánh này bị bỏ qua
                            // nên bấm "Thêm lô" xong lưu lại là lô biến mất.
                            createManualLot(itemId, whDto.getWarehouseId(), lotDto, staffId);
                            continue;
                        }
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

                // Chốt lại: tồn kho luôn phải bằng tổng số còn lại của các lô.
                syncInventoryWithLots(itemId, whDto.getWarehouseId(), inventory, staffId);
            }
        }

        CatalogItem saved = catalogItemRepo.saveCatalogItem(catalogItem);
        return catalogDtoMapper.toDto(saved);
    }

    // ─── Cấu hình đo lường / đóng gói / serial ─────────────────────────────────

    /**
     * Gộp cấu hình gửi lên với cấu hình đang có (field null = giữ nguyên), chuẩn hoá
     * rồi gán vào sản phẩm. itemId null nghĩa là đang tạo mới: chưa có tồn nên không
     * cần kiểm tra dữ liệu cũ, và đơn vị tính được dùng làm gợi ý kiểu đo lường.
     */
    private void applyMeasurementConfig(CatalogItem target, CatalogCreateDto dto, Integer itemId) {
        String requestedType = dto.getMeasurementType();
        Integer requestedScale = dto.getDecimalScale();
        if (itemId == null && requestedType == null && target.getUnit() != null) {
            ProductUnitJpa unit = productUnitJpaRepo.findByUnitName(target.getUnit().trim());
            if (unit != null) {
                requestedType = unit.getMeasurementType();
                if (requestedScale == null) requestedScale = unit.getDecimalScale();
            }
        }

        String previousType = target.getMeasurementType();
        Integer previousScale = target.getDecimalScale();
        boolean previousSerial = Boolean.TRUE.equals(target.getTracksSerial());

        ItemQuantityPolicy.MeasurementConfig config = itemQuantityPolicy.normalizeConfig(
                new ItemQuantityPolicy.MeasurementConfig(
                        requestedType != null ? requestedType : previousType,
                        requestedScale != null ? requestedScale : previousScale,
                        dto.getPackagingUnit() != null ? dto.getPackagingUnit() : target.getPackagingUnit(),
                        dto.getConversionFactor() != null ? dto.getConversionFactor() : target.getConversionFactor(),
                        dto.getSellByPackageOnly() != null ? dto.getSellByPackageOnly() : target.getSellByPackageOnly(),
                        dto.getTracksLot() != null ? dto.getTracksLot() : target.getTracksLot(),
                        dto.getTracksSerial() != null ? dto.getTracksSerial() : target.getTracksSerial()));

        if (itemId != null) {
            ItemQuantityPolicy.Rules newRules = new ItemQuantityPolicy.Rules(target.getItemName(), target.getUnit(),
                    config.measurementType(), config.decimalScale(), config.packagingUnit(),
                    config.conversionFactor(), config.sellByPackageOnly());
            boolean scaleShrinks = !config.measurementType().equals(ItemQuantityPolicy.measurementOf(previousType).name())
                    || (previousScale != null && config.decimalScale() < previousScale);
            if (scaleShrinks) {
                ensureStockFitsScale(itemId, newRules);
            }
            if (previousSerial && !config.tracksSerial()
                    && itemSerialJpaRepo.existsByItemIdAndStatus(itemId, com.g42.platform.gms.warehouse.domain.enums.SerialStatus.RESERVED)) {
                throw new WarehouseException("Đang có serial được giữ cho báo giá — bỏ chọn serial ở các báo giá đó rồi mới tắt theo dõi serial",
                        WarehouseErrorCode.INVALID_MEASUREMENT_CONFIG);
            }
        }

        target.setMeasurementType(config.measurementType());
        target.setDecimalScale(config.decimalScale());
        target.setPackagingUnit(config.packagingUnit());
        target.setConversionFactor(config.conversionFactor());
        target.setSellByPackageOnly(config.sellByPackageOnly());
        target.setTracksLot(config.tracksLot());
        target.setTracksSerial(config.tracksSerial());
    }

    /** Đổi sang ít chữ số lẻ hơn thì tồn kho và lô hiện có phải còn khớp cấu hình mới. */
    private void ensureStockFitsScale(Integer itemId, ItemQuantityPolicy.Rules rules) {
        for (InventoryJpa inventory : inventoryJpaRepo.findByItemIdOrderByQuantityDesc(itemId)) {
            boolean lotsFit = stockEntryItemJpaRepo.findFifoLots(inventory.getWarehouseId(), itemId).stream()
                    .noneMatch(lot -> ItemQuantityPolicy.exceedsScale(rules, lot.getRemainingQuantity()));
            if (ItemQuantityPolicy.exceedsScale(rules, inventory.getQuantity())
                    || ItemQuantityPolicy.exceedsScale(rules, inventory.getReservedQuantity())
                    || !lotsFit) {
                throw new WarehouseException("Không đổi được kiểu đo lường: tồn kho hiện có số lẻ ("
                        + Qty.text(inventory.getQuantity()) + ") không khớp cấu hình mới — điều chỉnh tồn trước",
                        WarehouseErrorCode.INVALID_MEASUREMENT_CONFIG);
            }
        }
    }

    /**
     * Hàng theo serial không được sửa số lượng tồn/lô bằng tay: số serial còn trong kho
     * phải khớp số còn lại của lô, nên tăng giảm hàng phải đi qua phiếu nhập/xuất/hoàn.
     */
    private void guardSerialStockEdit(InventoryJpa inventory, WarehouseUpdateDto whDto) {
        boolean quantityChanged = whDto.getQuantity() != null
                && !Qty.eq(whDto.getQuantity(), inventory.getQuantity());
        boolean lotChanged = false;
        if (whDto.getLots() != null) {
            for (LotUpdateDto lotDto : whDto.getLots()) {
                if (lotDto.getEntryItemId() == null) {
                    lotChanged = lotChanged || Qty.isPositive(lotDto.getRemainingQuantity());
                    continue;
                }
                if (lotDto.getRemainingQuantity() == null) continue;
                BigDecimal current = stockEntryItemJpaRepo.findById(lotDto.getEntryItemId())
                        .map(StockEntryItemJpa::getRemainingQuantity).orElse(null);
                lotChanged = lotChanged || !Qty.eq(current, lotDto.getRemainingQuantity());
            }
        }
        if (quantityChanged || lotChanged) {
            throw new WarehouseException("Sản phẩm theo dõi serial không sửa tay số lượng tồn/lô được — "
                    + "hãy nhập kho, xuất kho hoặc hoàn hàng để số serial luôn khớp tồn kho",
                    WarehouseErrorCode.INVALID_SERIAL);
        }
    }

    // ─── Tồn kho luôn đi kèm lô ────────────────────────────────────────────────
    //
    // Giá vốn, lãi gộp và FIFO đều đọc từ stock_entry_item. Tồn kho khai báo tay ở
    // popup "Chỉnh sửa danh mục & Tồn kho" mà không có lô nào thì lúc bán, dòng
    // xuất kho nhận entry_item_id = 0 → giá nhập và giá bán ghi nhận bằng 0, báo
    // cáo lãi kho bỏ sót toàn bộ và tồn kho trôi dần khỏi tổng số lô.
    //
    // Nên sau mỗi lần sửa tồn kho, phần chênh giữa số lượng khai báo và tổng số
    // còn lại của các lô được cân lại bằng một "lô điều chỉnh" của chính kho đó.

    /** Nhà cung cấp quy ước của phiếu nhập sinh tự động khi cân tồn kho. */
    private static final String ADJUSTMENT_SUPPLIER = "Điều chỉnh tồn kho";

    /** Tạo lô mới do người dùng thêm tay ở popup sửa tồn kho. */
    private void createManualLot(Integer itemId, Integer warehouseId, LotUpdateDto lotDto, Integer staffId) {
        BigDecimal quantity = Qty.nz(lotDto.getRemainingQuantity());
        if (warehouseId == null || quantity.signum() <= 0) {
            return;
        }

        String entryCode = lotDto.getEntryCode() != null ? lotDto.getEntryCode().trim() : "";
        StockEntryJpa entry = resolveOrCreateEntry(
                warehouseId,
                entryCode.isEmpty() ? null : entryCode,
                parseEntryDate(lotDto.getEntryDate()),
                "Nhập tay từ màn danh mục",
                staffId);

        BigDecimal importPrice = lotDto.getImportPrice() != null ? lotDto.getImportPrice() : BigDecimal.ZERO;
        StockEntryItemJpa lot = new StockEntryItemJpa();
        lot.setEntryId(entry.getEntryId());
        lot.setItemId(itemId);
        lot.setQuantity(quantity);
        lot.setRemainingQuantity(quantity);
        lot.setImportPrice(importPrice);
        lot.setMarkupMultiplier(resolveMarkup(lotDto, importPrice));
        lot.setMarkupMultiplierWholesale(lotDto.getMarkupMultiplierWholesale());
        stockEntryItemJpaRepo.save(lot);
    }

    /**
     * Cân tồn kho với tổng số còn lại của các lô.
     *
     * Thừa (tồn > lô): dồn phần chênh vào lô điều chỉnh của kho, giá nhập lấy theo
     * lô gần nhất để giá vốn không bị về 0.
     * Thiếu (lô > tồn): trừ bớt lô, ưu tiên lô điều chỉnh rồi tới lô mới nhất, để
     * các lô nhập thật cũ nhất giữ nguyên thứ tự FIFO và giá vốn.
     */
    private void syncInventoryWithLots(Integer itemId, Integer warehouseId, InventoryJpa inventory, Integer staffId) {
        if (warehouseId == null || inventory == null) {
            return;
        }
        BigDecimal target = Qty.nz(inventory.getQuantity());
        List<StockEntryItemJpa> lots = stockEntryItemJpaRepo.findFifoLots(warehouseId, itemId);
        BigDecimal lotTotal = Qty.sum(lots, StockEntryItemJpa::getRemainingQuantity);

        BigDecimal diff = target.subtract(lotTotal);
        if (diff.signum() == 0) {
            return;
        }

        if (diff.signum() > 0) {
            addToAdjustmentLot(itemId, warehouseId, diff, staffId);
        } else {
            reduceLots(lots, diff.negate());
        }

        logInventoryAdjustment(warehouseId, itemId, diff, target, staffId);
    }

    /** Dồn phần tồn dôi ra vào một lô điều chỉnh mới của kho. */
    private void addToAdjustmentLot(Integer itemId, Integer warehouseId, BigDecimal quantity, Integer staffId) {
        BigDecimal importPrice = BigDecimal.ZERO;
        BigDecimal markup = null;
        BigDecimal markupWholesale = null;
        List<StockEntryItemJpa> latest = stockEntryItemJpaRepo.findLatestLot(warehouseId, itemId);
        if (!latest.isEmpty()) {
            StockEntryItemJpa ref = latest.get(0);
            importPrice = ref.getImportPrice() != null ? ref.getImportPrice() : BigDecimal.ZERO;
            markup = ref.getMarkupMultiplier();
            markupWholesale = ref.getMarkupMultiplierWholesale();
        }

        StockEntryJpa entry = resolveOrCreateEntry(warehouseId, null, LocalDate.now(), ADJUSTMENT_SUPPLIER, staffId);
        StockEntryItemJpa lot = new StockEntryItemJpa();
        lot.setEntryId(entry.getEntryId());
        lot.setItemId(itemId);
        lot.setQuantity(quantity);
        lot.setRemainingQuantity(quantity);
        lot.setImportPrice(importPrice);
        lot.setMarkupMultiplier(markup != null ? markup : BigDecimal.ONE);
        lot.setMarkupMultiplierWholesale(markupWholesale);
        lot.setNotes("Lô điều chỉnh tự động khi sửa tồn kho ở màn danh mục");
        stockEntryItemJpaRepo.save(lot);
    }

    /**
     * Trừ bớt số còn lại của các lô khi tồn khai báo thấp hơn tổng lô.
     * Trừ từ lô mới nhất trở về trước để lô cũ nhất (đang đứng đầu hàng FIFO)
     * giữ nguyên số lượng và giá vốn.
     */
    private void reduceLots(List<StockEntryItemJpa> fifoLots, BigDecimal quantityToRemove) {
        BigDecimal remain = quantityToRemove;
        List<StockEntryItemJpa> newestFirst = new ArrayList<>(fifoLots);
        java.util.Collections.reverse(newestFirst);

        for (StockEntryItemJpa lot : newestFirst) {
            if (remain.signum() <= 0) break;
            BigDecimal available = Qty.nz(lot.getRemainingQuantity());
            if (available.signum() <= 0) continue;
            BigDecimal take = Qty.min(available, remain);
            lot.setRemainingQuantity(available.subtract(take));
            stockEntryItemJpaRepo.save(lot);
            remain = remain.subtract(take);
        }
    }

    /**
     * Lấy phiếu nhập theo mã, hoặc tạo phiếu mới ở trạng thái CONFIRMED.
     * Phải CONFIRMED thì lô mới lọt vào truy vấn FIFO và hiện ở popup chọn lô.
     */
    private StockEntryJpa resolveOrCreateEntry(Integer warehouseId, String entryCode, LocalDate entryDate, String supplierName, Integer staffId) {
        if (entryCode != null && !entryCode.isBlank()) {
            Optional<StockEntryJpa> existing = stockEntryJpaRepo.findByEntryCode(entryCode);
            if (existing.isPresent() && warehouseId.equals(existing.get().getWarehouseId())) {
                return existing.get();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        StockEntryJpa entry = new StockEntryJpa();
        entry.setEntryCode(entryCode != null && !entryCode.isBlank() && !stockEntryJpaRepo.existsByEntryCode(entryCode)
                ? entryCode
                : generateAdjustmentEntryCode());
        entry.setWarehouseId(warehouseId);
        entry.setSupplierName(supplierName);
        entry.setEntryDate(entryDate != null ? entryDate : LocalDate.now());
        entry.setStatus(com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus.CONFIRMED);
        entry.setNotes("Sinh tự động từ màn Chỉnh sửa danh mục & Tồn kho");
        entry.setCreatedBy(staffId);
        entry.setConfirmedBy(staffId);
        entry.setConfirmedAt(now);
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        return stockEntryJpaRepo.save(entry);
    }

    private String generateAdjustmentEntryCode() {
        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int seq = 1; seq < 10000; seq++) {
            String candidate = String.format("DC-%s-%d", date, seq);
            if (!stockEntryJpaRepo.existsByEntryCode(candidate)) {
                return candidate;
            }
        }
        return "DC-" + System.currentTimeMillis();
    }

    private LocalDate parseEntryDate(String raw) {
        if (raw == null || raw.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(raw.substring(0, Math.min(10, raw.length())));
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }

    private BigDecimal resolveMarkup(LotUpdateDto lotDto, BigDecimal importPrice) {
        if (lotDto.getMarkupMultiplier() != null) {
            return lotDto.getMarkupMultiplier();
        }
        if (lotDto.getSellingPrice() != null && importPrice != null && importPrice.compareTo(BigDecimal.ZERO) > 0) {
            return lotDto.getSellingPrice().divide(importPrice, 4, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ONE;
    }

    /** Ghi vết mọi lần tồn kho bị sửa tay — trước đây thao tác này không để lại dấu nào. */
    private void logInventoryAdjustment(Integer warehouseId, Integer itemId, BigDecimal diff, BigDecimal balanceAfter, Integer staffId) {
        if (staffId == null) {
            // inventory_transaction.created_by là NOT NULL; không xác định được người
            // thao tác thì bỏ qua phần ghi log chứ không làm hỏng cả thao tác sửa.
            return;
        }
        InventoryTransactionJpa tx = new InventoryTransactionJpa();
        tx.setWarehouseId(warehouseId);
        tx.setItemId(itemId);
        tx.setTransactionType(com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType.ADJUSTMENT);
        tx.setQuantity(diff);
        tx.setBalanceAfter(balanceAfter);
        tx.setReferenceType("catalog_inventory_edit");
        tx.setReferenceId(itemId);
        tx.setNotes("Sửa tồn kho ở màn Chỉnh sửa danh mục & Tồn kho");
        tx.setCreatedById(staffId);
        tx.setCreatedAt(Instant.now());
        inventoryTransactionJpaRepo.save(tx);
    }

    /**
     * Soft-delete a catalog item so stock, lots and transaction history remain
     * available for auditing. A linked sales article is deactivated as part of
     * the same transaction and therefore disappears from public sales pages.
     */
    @Transactional
    public void deactivateCatalogItem(Integer itemId) {
        CatalogItemJpa catalogItem = catalogItemJpaRepo.findById(itemId)
                .orElseThrow(() -> new WarehouseException(
                        "Catalog item not found", WarehouseErrorCode.CATALOG_404));

        catalogItem.setIsActive(false);
        catalogItemJpaRepo.save(catalogItem);

        Long serviceId = catalogItem.getServiceId();
        if (serviceId != null) {
            serviceJpaRepository.findById(serviceId).ifPresent(service -> {
                service.setStatus(ServiceStatus.INACTIVE);
                serviceJpaRepository.save(service);
            });
        }
    }

    /** Restore a soft-deleted catalog item and its still-linked sales article. */
    @Transactional
    public void activateCatalogItem(Integer itemId) {
        CatalogItemJpa catalogItem = catalogItemJpaRepo.findById(itemId)
                .orElseThrow(() -> new WarehouseException(
                        "Catalog item not found", WarehouseErrorCode.CATALOG_404));

        // Trong lúc bị xoá, SKU có thể đã được mục mới lấy lại — không cho hai mục đang hoạt động trùng SKU.
        // (Slug không cần kiểm tra: nếu đã bị lấy thì mục này đã bị gỡ slug về null.)
        if (catalogItem.getSku() != null && catalogItemRepo.existsActiveBySku(catalogItem.getSku(), itemId)) {
            throw new WarehouseException("SKU của mục này đã được mục khác dùng lại, không thể kích hoạt lại",
                    WarehouseErrorCode.DUPLICATE_SKU);
        }

        catalogItem.setIsActive(true);
        catalogItemJpaRepo.save(catalogItem);

        Long serviceId = catalogItem.getServiceId();
        if (serviceId != null) {
            serviceJpaRepository.findById(serviceId).ifPresent(service -> {
                service.setStatus(ServiceStatus.ACTIVE);
                serviceJpaRepository.save(service);
            });
        }
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
    public ProductUnitJpa createProductUnit(String unitName, String measurementType, Integer decimalScale) {
        String trimmed = unitName != null ? unitName.trim() : "";
        if (trimmed.isEmpty()) {
            throw new WarehouseException("Unit name cannot be empty", WarehouseErrorCode.INVALID_CATEGORY);
        }
        if (productUnitJpaRepo.existsByUnitName(trimmed)) {
            ProductUnitJpa existing = productUnitJpaRepo.findByUnitName(trimmed);
            if (existing.getIsActive() == 0) {
                existing.setIsActive((byte) 1);
                applyUnitMeasurement(existing, measurementType, decimalScale);
                return productUnitJpaRepo.save(existing);
            }
            throw new WarehouseException("Unit name already exists", WarehouseErrorCode.INVALID_CATEGORY);
        }
        ProductUnitJpa unit = new ProductUnitJpa();
        unit.setUnitName(trimmed);
        unit.setIsActive((byte) 1);
        applyUnitMeasurement(unit, measurementType, decimalScale);
        return productUnitJpaRepo.save(unit);
    }

    @Transactional
    public ProductUnitJpa updateProductUnit(Integer unitId, String measurementType, Integer decimalScale) {
        ProductUnitJpa unit = productUnitJpaRepo.findById(unitId)
                .orElseThrow(() -> new WarehouseException("Unit not found", WarehouseErrorCode.INVALID_CATEGORY));
        applyUnitMeasurement(unit, measurementType, decimalScale);
        return productUnitJpaRepo.save(unit);
    }

    private void applyUnitMeasurement(ProductUnitJpa unit, String measurementType, Integer decimalScale) {
        if (measurementType == null && decimalScale == null) return;
        ItemQuantityPolicy.MeasurementConfig normalized = itemQuantityPolicy.normalizeConfig(
                new ItemQuantityPolicy.MeasurementConfig(
                        measurementType != null ? measurementType : unit.getMeasurementType(),
                        decimalScale != null ? decimalScale : unit.getDecimalScale(),
                        null, null, false, true, false));
        unit.setMeasurementType(normalized.measurementType());
        unit.setDecimalScale(normalized.decimalScale());
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
