package com.g42.platform.gms.warehouse.app.service.catalog;


import com.g42.platform.gms.common.util.Qty;
import com.g42.platform.gms.warehouse.api.dto.CatalogDetailDto;
import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.api.dto.CatalogWarehouseDto;
import com.g42.platform.gms.warehouse.api.dto.WarehouseDetailDto;
import com.g42.platform.gms.warehouse.api.dto.WarehouseDto;
import com.g42.platform.gms.warehouse.api.dto.WarehouseLotDto;
import com.g42.platform.gms.warehouse.api.dto.request.CreateWarehouseRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateWarehouseRequest;
import com.g42.platform.gms.warehouse.api.mapper.CatalogDtoMapper;
import com.g42.platform.gms.warehouse.app.service.dto.PricingResolve;
import com.g42.platform.gms.warehouse.app.service.pricing.PricingService;
import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.ProductLine;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseDetailProjection;
import com.g42.platform.gms.warehouse.domain.repository.WarehousePricingRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryItemJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WarehouseService {
    @Autowired
    private WarehouseRepo warehouseRepo;
    @Autowired
    private CatalogItemRepo catalogItemRepo;
    @Autowired
    private CatalogItemService catalogItemService;
    @Autowired
    private CatalogDtoMapper catalogDtoMapper;
    @Autowired
    private PricingService  pricingService;
    @Autowired
    private StockEntryItemJpaRepo stockEntryItemJpaRepo;
    @Autowired
    private WarehousePricingRepo warehousePricingRepo;
    @Autowired
    private com.g42.platform.gms.warehouse.domain.repository.InventoryRepo inventoryRepo;

    // ─── CRUD Warehouse ───────────────────────────────────────────────────────

    /** Tạo kho mới (MASTER, BRANCH hoặc DEFECTIVE). */
    @Transactional
    public WarehouseDto createWarehouse(CreateWarehouseRequest req) {
        // validate code unique
        boolean codeExists = warehouseRepo.findAllWarehouses().stream()
                .anyMatch(w -> req.getWarehouseCode().equalsIgnoreCase(w.getWarehouseCode()));
        if (codeExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mã kho '" + req.getWarehouseCode() + "' đã tồn tại");
        }

        // BRANCH / DEFECTIVE phải có kho cha; MASTER luôn là gốc
        if (req.getWarehouseType() != com.g42.platform.gms.common.enums.WarehouseTypeEnum.MASTER) {
            if (req.getParentWarehouseId() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Kho loại " + req.getWarehouseType() + " phải có kho cha (parentWarehouseId)");
            }
            validateParent(req.getParentWarehouseId());
        } else if (req.getParentWarehouseId() != null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Kho tổng (MASTER) là kho gốc, không có kho cha");
        }

        // DEFECTIVE: không tạo trùng cho cùng 1 kho cha
        if (req.getWarehouseType() == com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE) {
            warehouseRepo.findByParentAndType(req.getParentWarehouseId(),
                            com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE)
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Kho lỗi (DEFECTIVE) đã tồn tại cho kho cha id=" + req.getParentWarehouseId());
                    });
        }

        Warehouse w = new Warehouse();
        w.setWarehouseCode(req.getWarehouseCode().trim().toUpperCase());
        w.setWarehouseName(req.getWarehouseName().trim());
        w.setWarehouseType(req.getWarehouseType());
        w.setParentWarehouseId(req.getParentWarehouseId());
        w.setAddress(req.getAddress());
        w.setManagerStaffId(req.getManagerStaffId());
        w.setIsActive(true);
        w.setCreatedAt(java.time.Instant.now());

        return toDto(warehouseRepo.save(w));
    }

    /** Cập nhật tên, địa chỉ, manager của kho. */
    @Transactional
    public WarehouseDto updateWarehouse(Integer warehouseId, UpdateWarehouseRequest req) {
        Warehouse w = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy kho id=" + warehouseId));

        if (req.getWarehouseName() != null && !req.getWarehouseName().isBlank()) {
            w.setWarehouseName(req.getWarehouseName().trim());
        }
        if (req.getAddress() != null) {
            w.setAddress(req.getAddress());
        }
        if (req.getManagerStaffId() != null) {
            w.setManagerStaffId(req.getManagerStaffId());
        }
        if (req.getParentWarehouseId() != null && !req.getParentWarehouseId().equals(w.getParentWarehouseId())) {
            moveUnderParent(w, req.getParentWarehouseId());
        }

        return toDto(warehouseRepo.save(w));
    }

    /** Kho cha phải tồn tại và không phải kho lỗi (kho lỗi là nút lá, không chứa kho con). */
    private Warehouse validateParent(Integer parentId) {
        Warehouse parent = warehouseRepo.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy kho cha id=" + parentId));
        if (parent.getWarehouseType() == com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể đặt kho con dưới kho lỗi '" + parent.getWarehouseName() + "'");
        }
        return parent;
    }

    /** Chuyển kho sang kho cha khác — chặn vòng lặp (chọn chính nó / kho con cháu làm cha). */
    private void moveUnderParent(Warehouse w, Integer newParentId) {
        if (w.getWarehouseType() == com.g42.platform.gms.common.enums.WarehouseTypeEnum.MASTER) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Kho tổng (MASTER) là kho gốc, không có kho cha");
        }
        validateParent(newParentId);
        WarehouseHierarchy tree = new WarehouseHierarchy(warehouseRepo.findAllWarehouses());
        if (tree.wouldCreateCycle(w.getWarehouseId(), newParentId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể chọn chính kho này hoặc kho con của nó làm kho cha");
        }
        if (w.getWarehouseType() == com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE) {
            warehouseRepo.findByParentAndType(newParentId, com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE)
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Kho cha id=" + newParentId + " đã có kho lỗi (DEFECTIVE)");
                    });
        }
        w.setParentWarehouseId(newParentId);
    }

    /** Bật/tắt trạng thái kho. */
    @Transactional
    public WarehouseDto setWarehouseActive(Integer warehouseId, boolean active) {
        Warehouse w = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy kho id=" + warehouseId));
        w.setIsActive(active);
        return toDto(warehouseRepo.save(w));
    }

    /** Lấy chi tiết 1 kho. */
    public WarehouseDto getWarehouse(Integer warehouseId) {
        return warehouseRepo.findById(warehouseId)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy kho id=" + warehouseId));
    }

    /** Danh sách kho đang hoạt động (isActive = false thì lấy kho đã tắt). */
    public java.util.List<WarehouseDto> listWarehouses(Boolean isActive) {
        return listWarehouses(isActive, false, false);
    }

    /**
     * Danh sách kho. Mặc định (isActive = null) chỉ trả kho đang hoạt động như trước vì các ô chọn
     * kho đều dùng endpoint này; includeInactive = true để màn cấu hình thấy cả kho đã tắt.
     * withStock = true thì kèm tồn riêng + tồn cộng dồn cây kho con
     * (đọc toàn bộ bảng inventory một lần nên chỉ bật ở màn cấu hình kho).
     */
    @Transactional(readOnly = true)
    public java.util.List<WarehouseDto> listWarehouses(Boolean isActive, boolean withStock, boolean includeInactive) {
        List<Warehouse> all = warehouseRepo.findAllWarehouses();
        Boolean activeFilter = isActive == null && !includeInactive ? Boolean.TRUE : isActive;
        List<WarehouseDto> result = all.stream()
                .filter(w -> activeFilter == null || activeFilter.equals(Boolean.TRUE.equals(w.getIsActive())))
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
        if (withStock) {
            fillStockRollup(result, all);
        }
        return result;
    }

    private void fillStockRollup(List<WarehouseDto> dtos, List<Warehouse> all) {
        // warehouseId -> (itemId -> quantity), chỉ giữ dòng còn hàng
        Map<Integer, Map<Integer, BigDecimal>> qtyByWarehouse = new HashMap<>();
        for (com.g42.platform.gms.warehouse.domain.entity.Inventory inv : inventoryRepo.findAll()) {
            if (inv.getQuantity() == null || inv.getQuantity().signum() <= 0) continue;
            qtyByWarehouse.computeIfAbsent(inv.getWarehouseId(), k -> new HashMap<>())
                    .merge(inv.getItemId(), inv.getQuantity(), BigDecimal::add);
        }
        WarehouseHierarchy tree = new WarehouseHierarchy(all);
        for (WarehouseDto dto : dtos) {
            Map<Integer, BigDecimal> own = qtyByWarehouse.getOrDefault(dto.getWarehouseId(), Map.of());
            dto.setOwnQuantity(own.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            dto.setOwnItemCount(own.size());

            BigDecimal total = BigDecimal.ZERO;
            Set<Integer> items = new HashSet<>();
            for (Integer wid : tree.rollupWarehouseIds(dto.getWarehouseId())) {
                Map<Integer, BigDecimal> m = qtyByWarehouse.getOrDefault(wid, Map.of());
                for (BigDecimal q : m.values()) total = total.add(q);
                items.addAll(m.keySet());
            }
            dto.setTotalQuantity(total);
            dto.setTotalItemCount(items.size());
        }
    }

    private WarehouseDto toDto(Warehouse w) {
        WarehouseDto dto = new WarehouseDto();
        dto.setWarehouseId(w.getWarehouseId());
        dto.setWarehouseCode(w.getWarehouseCode());
        dto.setWarehouseName(w.getWarehouseName());
        dto.setWarehouseType(w.getWarehouseType());
        dto.setParentWarehouseId(w.getParentWarehouseId());
        dto.setAddress(w.getAddress());
        dto.setManagerStaffId(w.getManagerStaffId());
        dto.setIsActive(w.getIsActive());
        dto.setCreatedAt(w.getCreatedAt());
        return dto;
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo kho hàng lỗi (DEFECTIVE) cho một chi nhánh.
     *
     * @param branchWarehouseId - ID kho chi nhánh chính
     */
    @Transactional
    public void createDefectiveWarehouse(Integer branchWarehouseId) {
        com.g42.platform.gms.warehouse.domain.entity.Warehouse parentWarehouse = warehouseRepo.findById(branchWarehouseId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Không tìm thấy kho với ID: " + branchWarehouseId
                ));

        Optional<com.g42.platform.gms.warehouse.domain.entity.Warehouse> existing =
                warehouseRepo.findByParentAndType(branchWarehouseId, com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE);

        if (existing.isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Kho DEFECTIVE đã tồn tại cho warehouse ID: " + branchWarehouseId +
                            " (Kho lỗi ID: " + existing.get().getWarehouseId() + ")"
            );
        }

        com.g42.platform.gms.warehouse.domain.entity.Warehouse defectiveWarehouse =
                new com.g42.platform.gms.warehouse.domain.entity.Warehouse();

        defectiveWarehouse.setWarehouseCode(parentWarehouse.getWarehouseCode() + "-LOI");
        defectiveWarehouse.setWarehouseName("Kho hàng lỗi - " + parentWarehouse.getWarehouseName());
        defectiveWarehouse.setWarehouseType(com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE);
        defectiveWarehouse.setParentWarehouseId(branchWarehouseId);
        defectiveWarehouse.setAddress(parentWarehouse.getAddress());
        defectiveWarehouse.setManagerStaffId(parentWarehouse.getManagerStaffId());
        defectiveWarehouse.setIsActive(true);
        defectiveWarehouse.setCreatedAt(java.time.Instant.now());

        warehouseRepo   .save(defectiveWarehouse);
    }


    public Page<CatalogSummaryDto> getListItems(int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brand, Integer productLine, String categoryCode, BigDecimal minPrice, BigDecimal maxPrice, String sortBy, String vehicleBrand, String vehicleModel) {
        Integer resolvedCategoryId = null;
        if (categoryCode != null) {
            resolvedCategoryId = catalogItemService.findCodeByCategoryCode(categoryCode);
        }
        Page<CatalogItem> catalogItems = warehouseRepo.getListOfCatalogItems(page,size,itemType,isActive,search,brand,productLine,resolvedCategoryId,minPrice,maxPrice,sortBy,vehicleBrand,vehicleModel);
        //get all ids of brands and lineProduct to query find string
        Set<Integer> brandIds = catalogItems.stream()
                .map(CatalogItem::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> lineIds = catalogItems.stream()
                .map(CatalogItem::getProductLineId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> categoryIds = catalogItems.stream()
                .map(CatalogItem::getItemCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Integer, String> brandMap = catalogItemRepo.getAllBrandByIds(brandIds);

        Map<Integer, String> lineMap = catalogItemRepo.findAllLinesByIds(lineIds);

        Map<Integer, String> cateMap = catalogItemRepo.findAllCatesByIds(categoryIds);
        return catalogItems.map(catalogItem -> {
            CatalogSummaryDto dto = catalogDtoMapper.toSumaryDto(catalogItem);
            if (catalogItem.getBrandId() != null) {
                dto.setBrand(brandMap.get(catalogItem.getBrandId()));
            }
            if (catalogItem.getProductLineId() != null) {
                dto.setProductLine(lineMap.get(catalogItem.getProductLineId()));
            }
            if (catalogItem.getItemCategoryId() != null) {
                dto.setItemCategoryCode(cateMap.get(catalogItem.getItemCategoryId()));
            }
            return dto;
        });
    }
    public Page<CatalogWarehouseDto> getListItemsDetail(int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brand, Integer productLine, String categoryCode, BigDecimal minPrice, BigDecimal maxPrice, String sortBy, String vehicleBrand, String vehicleModel) {
        Integer resolvedCategoryId = null;
        if (categoryCode != null) {
            resolvedCategoryId = catalogItemService.findCodeByCategoryCode(categoryCode);
        }
        Page<CatalogItem> catalogItems = warehouseRepo.getListOfCatalogItems(page,size,itemType,isActive,search,brand,productLine,resolvedCategoryId,minPrice,maxPrice,sortBy,vehicleBrand,vehicleModel);
        //get all ids of brands and lineProduct to query find string
        Set<Integer> brandIds = catalogItems.stream()
                .map(CatalogItem::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> lineIds = catalogItems.stream()
                .map(CatalogItem::getProductLineId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> categoryIds = catalogItems.stream()
                .map(CatalogItem::getItemCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> itemIds = catalogItems.stream()
                .map(CatalogItem::getItemId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Integer, String> brandMap = catalogItemRepo.getAllBrandByIds(brandIds);

        Map<Integer, String> lineMap = catalogItemRepo.findAllLinesByIds(lineIds);

        Map<Integer, String> cateMap = catalogItemRepo.findAllCatesByIds(categoryIds);
        Map<Integer, String> cateNameMap = catalogItemRepo.findAllCateNamesByIds(categoryIds);

        Map<Integer, List<WarehouseDetailDto>> itemWarehouseMap;

        if (!itemIds.isEmpty()) {
            // Query 1 lần lấy toàn bộ thông tin kho của các item trên trang hiện tại
            List<WarehouseDetailProjection> warehouseProjections = warehouseRepo.getWarehouseDetailsByItemIds(itemIds);

            itemWarehouseMap = warehouseProjections.stream()
                    .collect(Collectors.groupingBy(
                            WarehouseDetailProjection::getItemId, // Nhóm theo itemId
                            Collectors.mapping(prj -> new WarehouseDetailDto(
                                    prj.getWarehouseId(),
                                    prj.getWarehouseCode(),
                                    prj.getWarehouseName(),
                                    prj.getWarehouseAddress(),
                                    prj.getItemId(),
                                    prj.getSellingPrice(),
                                    prj.getQuantity(),
                                    prj.getReservedQuantity(),
                                    prj.getMinStockLevel(),
                                    prj.getMaxStockLevel(),
                                    prj.getAvailableStockLevel(),
                                    prj.getNotify()
                            ), Collectors.toList()) // Map Projection thành DTO và gom thành List
                    ));
        } else {
            itemWarehouseMap = new HashMap<>();
        }

        // Tải sẵn giá kho + lô hàng cho CẢ trang bằng 3 query, thay vì ~5 query cho mỗi
        // cặp kho × vật tư (trang 500 dòng từng tốn hàng nghìn query, ~6-7 giây).
        Map<WarehouseItemKey, WarehousePricing> pricingMap = new HashMap<>();
        Map<WarehouseItemKey, List<WarehouseLotDto>> lotsMap = new HashMap<>();
        Map<WarehouseItemKey, Object[]> latestLotMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            for (WarehousePricing p : warehousePricingRepo.findActiveByItemIds(itemIds)) {
                pricingMap.putIfAbsent(new WarehouseItemKey(p.getWarehouseId(), p.getItemId()), p);
            }
            for (Object[] r : stockEntryItemJpaRepo.findWarehouseLotRowsByItemIds(itemIds)) {
                WarehouseLotDto lot = new WarehouseLotDto(
                        (Integer) r[2], (Integer) r[3], (String) r[4],
                        (BigDecimal) r[5], (BigDecimal) r[6], (BigDecimal) r[7],
                        (BigDecimal) r[8], (BigDecimal) r[9],
                        (java.time.LocalDate) r[10], (java.time.LocalDate) r[11],
                        null, null);
                lotsMap.computeIfAbsent(new WarehouseItemKey((Integer) r[0], (Integer) r[1]), k -> new ArrayList<>()).add(lot);
            }
            for (Object[] r : stockEntryItemJpaRepo.findLatestLotRowsByItemIds(itemIds)) {
                latestLotMap.put(new WarehouseItemKey((Integer) r[0], (Integer) r[1]), r);
            }
        }

        return catalogItems.map(catalogItem -> {
            CatalogWarehouseDto dto = catalogDtoMapper.toSumaryWarehouseDto(catalogItem);
            if (catalogItem.getBrandId() != null) {
                dto.setBrand(brandMap.get(catalogItem.getBrandId()));
            }
            if (catalogItem.getProductLineId() != null) {
                dto.setProductLine(lineMap.get(catalogItem.getProductLineId()));
            }
            if (catalogItem.getItemCategoryId() != null) {
                dto.setItemCategoryCode(cateMap.get(catalogItem.getItemCategoryId()));
                dto.setItemCategoryName(cateNameMap.get(catalogItem.getItemCategoryId()));
            }
            List<WarehouseDetailDto> details = itemWarehouseMap.getOrDefault(catalogItem.getItemId(), new ArrayList<>());
            dto.setWarehouseDetails(details);
            for (WarehouseDetailDto detail : details) {
                WarehouseItemKey key = new WarehouseItemKey(detail.getWarehouseId(), catalogItem.getItemId());
                WarehousePricing pricing = pricingMap.get(key);

                PricingResolve pricingResolve = resolveEffectivePrice(catalogItem, detail.getWarehouseId(), pricing, latestLotMap.get(key));
                detail.setSellingPrice(pricingResolve.getFinalPrice());
                detail.setNotify(pricingResolve.getNotify());

                List<WarehouseLotDto> lots = lotsMap.getOrDefault(key, new ArrayList<>());

                // Lấy cấu hình giá bán cố định nếu có
                BigDecimal warehouseSellingPrice = pricing != null ? pricing.getSellingPrice() : null;
                BigDecimal warehouseSellingPriceWholesale = pricing != null ? pricing.getSellingPriceWholesale() : null;

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
                detail.setLots(lots);
            }
            return dto;
        });
    }

    private record WarehouseItemKey(Integer warehouseId, Integer itemId) {}

    /**
     * Cùng thứ tự ưu tiên với PricingService.getEffectivePrice nhưng dùng dữ liệu đã tải sẵn:
     * 1. giá cài đặt ở warehouse_pricing, 2. giá nhập lô mới nhất × hệ số, 3. giá niêm yết.
     * Lô mới nhất thiếu hệ số (phải tra cấu hình fallback theo loại hàng) thì gọi lại
     * PricingService — trường hợp hiếm nên không đáng gộp.
     */
    private PricingResolve resolveEffectivePrice(CatalogItem catalogItem, Integer warehouseId,
                                                 WarehousePricing pricing, Object[] latestLot) {
        PricingResolve resolve = new PricingResolve();
        if (pricing != null && pricing.getSellingPrice() != null
                && pricing.getSellingPrice().compareTo(BigDecimal.ZERO) > 0) {
            resolve.setFinalPrice(pricing.getSellingPrice());
            resolve.setNotify("Đang dùng giá cài đặt sẵn");
            return resolve;
        }
        if (latestLot != null) {
            BigDecimal importPrice = (BigDecimal) latestLot[2];
            BigDecimal multiplier = (BigDecimal) latestLot[3];
            if (importPrice == null || multiplier == null) {
                return pricingService.getEffectivePrice(catalogItem.getItemId(), warehouseId, catalogItem.getPrice());
            }
            BigDecimal lotPrice = importPrice.multiply(multiplier);
            if (lotPrice.compareTo(BigDecimal.ZERO) > 0) {
                resolve.setFinalPrice(lotPrice);
                return resolve;
            }
        }
        BigDecimal price = catalogItem.getPrice();
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            resolve.setFinalPrice(price);
            resolve.setNotify("Đang dùng giá niêm yết của sản phẩm");
            return resolve;
        }
        resolve.setFinalPrice(BigDecimal.ZERO);
        resolve.setNotify("Không tìm thấy giá phù hợp trong cơ sở dữ liệu");
        return resolve;
    }
}
