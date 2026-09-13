package com.g42.platform.gms.estimation.app.service;


import com.g42.platform.gms.common.util.Qty;
import com.g42.platform.gms.auth.api.internal.CustomerInternalApi;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.booking_management.api.internal.BookingManageInternalApi;
import org.apache.commons.lang3.tuple.Pair;
import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.estimation.api.dto.*;
import com.g42.platform.gms.estimation.api.dto.request.EstimateItemReqDto;
import com.g42.platform.gms.estimation.api.dto.request.EstimateRequestDto;
import com.g42.platform.gms.estimation.api.internal.TaxRuleInternalApi;
import com.g42.platform.gms.estimation.api.mapper.EstimateDtoMapper;
import com.g42.platform.gms.estimation.api.mapper.StockAllocationDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.*;
import com.g42.platform.gms.estimation.domain.exception.EstimateErrorCode;
import com.g42.platform.gms.estimation.domain.exception.EstimateException;
import com.g42.platform.gms.estimation.domain.repository.*;
import com.g42.platform.gms.promotion.api.internal.PromotionInternalApi;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.entity.PromotionBuyItem;
import com.g42.platform.gms.promotion.domain.entity.PromotionGiftItem;
import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.api.internal.WarehouseInternalApi;
import com.g42.platform.gms.warehouse.api.mapper.WarehouseDtoMapper;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.infrastructure.repository.FallbackPricingConfigJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.FallbackPricingConfigJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import com.g42.platform.gms.estimation.domain.enums.EstimateTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstimateService {
    private final EstimateRepository estimateRepository;
    private final EstimateItemRepository estimateItemRepository;
    private final ItemCategoryRepository itemCategoryRepo;
    private final EstimateDtoMapper estimateDtoMapper;
    private final TaxRuleRepository taxRuleRepository;

    private final WarehouseInternalApi warehouseInternalApi;
    private final TaxRuleInternalApi taxRuleInternalApi;
    private final WarehouseDtoMapper warehouseDtoMapper;

    private final PromotionInternalApi promotionInternalApi;
    private final StockAllocationRepository stockAllocationRepository;
    private final StockAllocationDtoMapper stockAllocationDtoMapper;
    private final BookingManageInternalApi bookingManageInternalApi;

    private final FallbackPricingConfigJpaRepo fallbackPricingConfigJpaRepo;
    private final StockEntryItemJpaRepo stockEntryItemJpaRepo;
    private final CustomerInternalApi customerInternalApi;
    private final com.g42.platform.gms.warehouse.app.service.serial.ItemSerialService itemSerialService;
    private final com.g42.platform.gms.warehouse.app.service.catalog.ItemQuantityPolicy itemQuantityPolicy;
    private final com.g42.platform.gms.warehouse.infrastructure.repository.ItemSerialJpaRepo itemSerialJpaRepo;
    private final com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo catalogItemJpaRepo;


    public List<EstimateRespondDto> getEstimateByCode(Integer serviceTicketId) {
        // 1. Tìm tất cả estimate
        List<Estimate> estimateList = estimateRepository.getListOfEstimateByServiceTiketCode(serviceTicketId);

        // EARLY RETURN: Nếu không có estimate nào, trả về list rỗng ngay lập tức
        if (estimateList == null || estimateList.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Lấy danh sách estimate IDs
        List<Integer> estimateIds = estimateList.stream().map(Estimate::getId).toList();

        // 3. Lấy estimate items và lọc những item không bị xóa
        List<EstimateItem> estimateItems = estimateItemRepository.findByEstimateIds(estimateIds)
                .stream()
                .filter(item -> Boolean.FALSE.equals(item.getIsRemoved()))
                .toList();
//        List<Integer> estimateItemIds = estimateItems.stream().map(EstimateItem::getId).toList();
        List<Integer> warehouseIds = estimateItems.stream()
                .map(EstimateItem::getWarehouseId)
//                .filter(Objects::nonNull)
                .distinct()
                .toList();
        // 4. Lấy danh sách work-catalog của estimateItem an toàn
        List<Integer> itemCategoryIds = estimateItems.stream()
                .map(EstimateItem::getItemCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Khởi tạo map rỗng, chỉ query DB nếu có itemCategoryIds
        Map<Integer, ItemCategory> categoryMap;
        if (!itemCategoryIds.isEmpty()) {
            categoryMap = itemCategoryRepo.findAllById(itemCategoryIds)
                    .stream()
                    .collect(Collectors.toMap(ItemCategory::getId, wc -> wc));
        } else {
            categoryMap = new HashMap<>();
        }
        Map<Integer, Warehouse> warehouseMap;
        if (!warehouseIds.isEmpty()) {
            warehouseMap = warehouseInternalApi.findAllById(warehouseIds)
                    .stream()
                    .collect(Collectors.toMap(Warehouse::getWarehouseId, wc -> wc));
        } else {
            warehouseMap = new HashMap<>();
        }

        Map<Integer, StockAllocation> allocationMap;
        if (!estimateItems.isEmpty()) {
            allocationMap = stockAllocationRepository.findAllByEstimateId(estimateItems)
                    .stream()
                    .collect(Collectors.toMap(StockAllocation::getEstimateItemId, stockAllocation -> stockAllocation));
        } else {
            allocationMap = new HashMap<>();
        }

        // 5. Group estimateItem by estimateId
        Map<Integer, List<EstimateItem>> itemsByEstimateId = estimateItems.stream()
                .collect(Collectors.groupingBy(EstimateItem::getEstimateId));

        // 6. Map dữ liệu sang DTO
        return estimateList.stream().map(estimate -> {
            EstimateRespondDto dto = estimateDtoMapper.toEstimateDto(estimate);
            BigDecimal oldPrice = dto.getTotalPrice();
            List<EstimateItem> items = itemsByEstimateId.getOrDefault(estimate.getId(), List.of());

            BigDecimal totalTax = BigDecimal.ZERO;
            BigDecimal subTotal = BigDecimal.ZERO;
            BigDecimal finalPrice =  BigDecimal.ZERO;
            List<EstimateItemDto> itemDtos = new ArrayList<>();
            Set<Integer> prmotionIds = new HashSet<>();

            for (EstimateItem item : items) {
                EstimateItemDto itemDto = estimateDtoMapper.toEstimateItemDto(item);

                // inject work category to each item
                if (item.getItemCategoryId() != null) {
                    ItemCategory wc = categoryMap.get(item.getItemCategoryId());
                    if (wc != null) {
                        itemDto.setItemCategory(estimateDtoMapper.toItemCateDto(wc));
                    }
                }

                if (item.getWarehouseId() != null) {
                    Warehouse wc = warehouseMap.get(item.getWarehouseId());
                    if (wc != null) {
                        itemDto.setWarehouse(warehouseDtoMapper.toDtoInternal(wc));
                    }
                }

                if (item.getId() != null) {
                    StockAllocation allocation = allocationMap.get(item.getId());
                    if (allocation != null) {
                        Pair<Integer, String> returnPair = warehouseInternalApi.getReturnStatusByAlloId(allocation.getAllocationId());

                        StockAllocationDto allocationDto = stockAllocationDtoMapper.toDto(allocation);
                        if (returnPair!=null){

                        allocationDto.setReturnStatus(returnPair.getRight());
                        allocationDto.setReturnId(returnPair.getLeft());
                        }

                        itemDto.setStockAllocation(allocationDto);
                    }
                }

                // Tính toán giá tiền cho các item được checked
                if (Boolean.TRUE.equals(item.getIsChecked())) {
                    // 1. Cộng dồn tiền thuế
                    if (item.getTaxAmount() != null) {
                        totalTax = totalTax.add(item.getTaxAmount());
                    }

                    // 2. Cộng dồn tiền hàng
                    BigDecimal itemQty = Qty.nz(item.getQuantity());
                    BigDecimal itemPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    subTotal = subTotal.add(itemPrice.multiply(itemQty));
                    BigDecimal itemFinalPrice = item.getFinalPrice() != null ? item.getFinalPrice() : BigDecimal.ZERO;
                    if (item.getPromotionId() != null) {
                    prmotionIds.add(item.getPromotionId());
                    }
                    finalPrice = finalPrice.add(itemFinalPrice);
                }
                itemDtos.add(itemDto);
            }

            dto.setTotalPrice(finalPrice);
            dto.setSubTotal(subTotal);
            dto.setTotalTaxAmount(totalTax);
            fillSerialCodes(itemDtos);
            dto.setItems(itemDtos);
            dto.setPromotions(new ArrayList<>(prmotionIds));
            if (!oldPrice.equals(dto.getTotalPrice())) {
                Estimate newEstimate = estimateRepository.findEstimateById(dto.getEstimateId());
                newEstimate.setTotalPrice(dto.getTotalPrice());
                estimateRepository.save(newEstimate);
            }

            return dto;
        }).toList();
    }
    @Transactional
    public EstimateRespondDto createEstimate(EstimateRequestDto request) {
        Estimate estimate = new Estimate();
        estimate.setServiceTicketId(request.getServiceTicketId());
        estimate.setEstimateType(request.getEstimateType());
        estimate.setStatus(EstimateEnum.DRAFT);
        //todo: check version
        Integer revisedEstimateId = null;
        int latestEstimateVersion = estimateRepository.findLatestEstimate(request.getServiceTicketId());
        if (latestEstimateVersion > 1) {
        revisedEstimateId = estimateRepository.findEstimateIdByVersionAndServiceTicket(request.getServiceTicketId(),latestEstimateVersion-1);
        }
            System.out.println("DEBUG: revisedEstimateId=" + revisedEstimateId);
            System.out.println("DEBUG: latestEstimateVersion=" + latestEstimateVersion);
        estimate.setVersion(latestEstimateVersion);
        estimate.setTotalPrice(BigDecimal.ZERO);
        estimate.setRevisedFromId(revisedEstimateId);
        estimate.setFallbackPricingConfigId(request.getFallbackPricingConfigId());
        estimate.setManualMarkupMultiplier(request.getManualMarkupMultiplier());
        Estimate saved = estimateRepository.save(estimate);

        List<EstimateItem> items = resolveItems(request.getItems(), saved.getId(), request.getFallbackPricingConfigId(), request.getManualMarkupMultiplier(), request.getEstimateType());
        estimateItemRepository.saveAll(items);
        syncSerialReservations(saved.getId());

        //todo: update total_price
        BigDecimal totalPrice = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsChecked()))
                .filter(item -> Boolean.FALSE.equals(item.getIsRemoved()))
                .map(item -> item.getFinalPrice() != null ? item.getFinalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        saved.setTotalPrice(totalPrice);
        System.out.println("Total_price: " + totalPrice); // dùng biến totalPrice trực tiếp
        estimateRepository.save(saved);

        return getEstimateRespondDto(saved.getId());
    }

    @Transactional
    public EstimateRespondDto updateEstimate(Integer estimateId, EstimateRequestDto request) {
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        if (estimate == null) throw new RuntimeException("Estimate not found");
        estimate.setFallbackPricingConfigId(request.getFallbackPricingConfigId());
        estimate.setManualMarkupMultiplier(request.getManualMarkupMultiplier());

        List<EstimateItem> estimateItems = estimateItemRepository.findByEstimateId(estimateId);
        Map<Integer, EstimateItem> existingMap = estimateItems.stream()
                .filter(i -> i.getId() != null)
                .collect(Collectors.toMap(EstimateItem::getId, i -> i));
        List<EstimateItem> toSave = new ArrayList<>();
        Set<Integer> incomingIds = new HashSet<>();
        for (EstimateItemReqDto req : request.getItems()) {
            if (req.getEstimateItemId() != null && existingMap.containsKey(req.getEstimateItemId())) {
                // update old items
                EstimateItem existing = existingMap.get(req.getEstimateItemId());
                existing.setItemName(req.getItemName());
                existing.setItemId(req.getItemId());
                existing.setQuantity(req.getQuantity());
                validateLineQuantity(req);
                existing.setSerialIdsJson(itemSerialService.toJson(req.getSerialIds()));
                
                // Recalculate unit price using config if provided, otherwise use request unit price
                // Dùng warehouseId mới nhất từ request (chưa được set vào existing ở dưới) để tra đúng lô hàng.
                BigDecimal calculatedUnitPrice = calculateMarkupUnitPrice(
                    existing.getItemId(),
                    req.getWarehouseId(),
                    request.getFallbackPricingConfigId(),
                    request.getManualMarkupMultiplier(),
                    request.getEstimateType(),
                    req.getUnitPrice()
                );
                existing.setUnitPrice(calculatedUnitPrice);

                existing.setItemCategoryId(req.getItemCategoryId());
                existing.setCategoryLabel(normalizeCategoryLabel(req.getCategoryLabel()));
                existing.setWarehouseId(req.getWarehouseId());
                existing.setEntryItemId(req.getEntryItemId());
                existing.setIsChecked(req.getIsChecked());
                existing.setIsRemoved(req.getIsRemoved());
                existing.setIsGift(req.getIsGift());
                existing.setTriggeredByItemId(req.getTriggeredByItemId());
                existing.setDiscountAmount(req.getDiscountAmount());
                applyOutsourceAndNote(existing, req);
                ItemCategory wc = null;
                if (req.getItemCategoryId() != null) {
                    wc = itemCategoryRepo.findById(req.getItemCategoryId());
                }
                
                BigDecimal quantity = Qty.nz(existing.getQuantity());
                BigDecimal unitPrice = existing.getUnitPrice() != null ? existing.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal totalPrice = unitPrice.multiply(quantity);
                existing.setTotalPrice(totalPrice);
                existing.setTaxAmount(BigDecimal.ZERO);
                existing.setAppliedTaxRate(BigDecimal.ZERO);

                Integer ruleId = null;
                if (existing.getItemId() != null) {
                    CatalogItemDto itemDto = warehouseInternalApi.getItemInfo(existing.getItemId());
                    if (itemDto != null && itemDto.getTaxRuleId() != null) {
                        ruleId = itemDto.getTaxRuleId();
                    }
                }
                if (ruleId == null && wc != null && wc.getTaxRuleId() != null) {
                    ruleId = wc.getTaxRuleId();
                }
                if (ruleId == null && req.getTaxRuleId() != null) {
                    ruleId = req.getTaxRuleId();
                }

                applyTax(existing, ruleId);

                if (Boolean.TRUE.equals(existing.getIsGift())) {
                    existing.setFinalPrice(BigDecimal.ZERO);
                    existing.setIsOverridden(false);
                    existing.setManualLineTotal(null);
                } else if (!applyManualLineTotal(existing, req)) {
                    existing.setFinalPrice(existing.getTotalPrice());
                }

                toSave.add(existing);
                incomingIds.add(req.getEstimateItemId());
            } else {
                toSave.addAll(resolveItems(List.of(req), estimateId, request.getFallbackPricingConfigId(), request.getManualMarkupMultiplier(), request.getEstimateType()));
            }
        }
        estimateItems.stream()
                .filter(i -> !incomingIds.contains(i.getId()))
                .forEach(i -> {
                    i.setIsRemoved(true);
                    estimateItemRepository.save(i);
                });

        estimateItemRepository.saveAll(toSave);
        syncSerialReservations(estimateId);
        BigDecimal totalPrice = toSave.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsChecked()))
                .filter(item -> Boolean.FALSE.equals(item.getIsRemoved()))
                .map(item -> item.getFinalPrice() != null ? item.getFinalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        estimate.setEstimateType(request.getEstimateType());
        estimate.setTotalPrice(totalPrice);
        estimateRepository.save(estimate);

        return getEstimateRespondDto(estimateId);
    }
    /**
     * Chép phần thuê ngoài, ghi chú và chiết khấu phần trăm từ request sang dòng báo giá.
     * Dùng chung cho cả nhánh tạo mới và nhánh cập nhật để hai nhánh không lệch nhau.
     */
    private void applyOutsourceAndNote(EstimateItem item, EstimateItemReqDto req) {
        boolean isOutsource = Boolean.TRUE.equals(req.getIsOutsource());
        item.setIsOutsource(isOutsource);
        // Bỏ chọn thuê ngoài thì dọn luôn dữ liệu đi kèm, tránh còn sót đối tác cũ
        item.setOutsourcePartnerId(isOutsource ? req.getOutsourcePartnerId() : null);
        item.setOutsourceWorkContent(isOutsource ? req.getOutsourceWorkContent() : null);
        item.setLaborCost(isOutsource ? req.getLaborCost() : null);
        item.setNote(req.getNote());
        item.setDiscountPercent(req.getDiscountPercent());
    }

    /**
     * Số lượng dòng báo giá phải đúng kiểu đo lường của sản phẩm: hàng đếm là số nguyên,
     * hàng đo (lít, kg) đúng số chữ số lẻ, hàng chỉ bán nguyên hộp là bội số của hộp.
     * Dòng gõ tay không gắn sản phẩm chỉ cần dương và tối đa 3 chữ số lẻ.
     */
    private void validateLineQuantity(EstimateItemReqDto req) {
        if (req.getQuantity() == null || Boolean.TRUE.equals(req.getIsRemoved())) return;
        if (req.getItemId() != null) {
            itemQuantityPolicy.validateSaleQuantity(req.getItemId(), req.getQuantity(), req.getItemName());
        } else if (req.getQuantity().signum() <= 0 || req.getQuantity().stripTrailingZeros().scale() > Qty.SCALE) {
            throw new EstimateException((req.getItemName() != null ? req.getItemName() + ": " : "")
                    + "số lượng phải lớn hơn 0 và tối đa " + Qty.SCALE + " chữ số thập phân", EstimateErrorCode.BAD_REQUEST);
        }
    }

    /**
     * Giữ đúng các serial mà dòng báo giá đã chọn (sau khi các dòng đã có id).
     * Các bản báo giá cũ hơn của cùng phiếu coi như bị bản này thay thế.
     */
    private void syncSerialReservations(Integer estimateId) {
        List<EstimateItem> items = estimateItemRepository.findByEstimateId(estimateId);
        if (items.isEmpty()) return;

        Set<Integer> superseded = new HashSet<>();
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        if (estimate != null && estimate.getServiceTicketId() != null) {
            for (Estimate other : estimateRepository.getListOfEstimateByServiceTiketCode(estimate.getServiceTicketId())) {
                if (other.getId() != null && !other.getId().equals(estimateId)) {
                    estimateItemRepository.findByEstimateId(other.getId()).forEach(i -> superseded.add(i.getId()));
                }
            }
        } else if (estimate != null) {
            Integer previousId = estimate.getRevisedFromId();
            int guard = 20;
            while (previousId != null && guard-- > 0) {
                estimateItemRepository.findByEstimateId(previousId).forEach(i -> superseded.add(i.getId()));
                Estimate previous = estimateRepository.findEstimateById(previousId);
                previousId = previous != null ? previous.getRevisedFromId() : null;
            }
        }

        List<com.g42.platform.gms.warehouse.app.service.serial.ItemSerialService.EstimateSerialLine> lines = items.stream()
                .map(i -> new com.g42.platform.gms.warehouse.app.service.serial.ItemSerialService.EstimateSerialLine(
                        i.getId(), i.getItemId(), i.getWarehouseId(), i.getEntryItemId(), i.getQuantity(),
                        itemSerialService.parseIds(i.getSerialIdsJson()), Boolean.TRUE.equals(i.getIsRemoved())))
                .toList();
        itemSerialService.syncEstimateReservations(lines, superseded);
    }

    /** Điền mã serial và cấu hình đo lường của sản phẩm cho các dòng, gom truy vấn cho cả báo giá. */
    private void fillSerialCodes(List<EstimateItemDto> itemDtos) {
        Set<Integer> catalogIds = itemDtos.stream().map(EstimateItemDto::getItemId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!catalogIds.isEmpty()) {
            Map<Integer, com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa> catalogById =
                    catalogItemJpaRepo.findAllById(catalogIds).stream()
                            .collect(Collectors.toMap(c -> c.getItemId(), c -> c, (a, b) -> a));
            for (EstimateItemDto dto : itemDtos) {
                var catalog = dto.getItemId() == null ? null : catalogById.get(dto.getItemId());
                if (catalog == null) continue;
                dto.setMeasurementType(catalog.getMeasurementType());
                dto.setDecimalScale(catalog.getDecimalScale());
                dto.setPackagingUnit(catalog.getPackagingUnit());
                dto.setConversionFactor(catalog.getConversionFactor());
                dto.setSellByPackageOnly(catalog.getSellByPackageOnly());
                dto.setTracksLot(catalog.getTracksLot());
                dto.setTracksSerial(catalog.getTracksSerial());
            }
        }

        Set<Integer> ids = itemDtos.stream()
                .filter(d -> d.getSerialIds() != null)
                .flatMap(d -> d.getSerialIds().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return;
        Map<Integer, String> codeById = itemSerialJpaRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(s -> s.getSerialId(), s -> s.getSerialCode()));
        for (EstimateItemDto dto : itemDtos) {
            if (dto.getSerialIds() == null || dto.getSerialIds().isEmpty()) continue;
            dto.setSerialCodes(dto.getSerialIds().stream().map(codeById::get).filter(Objects::nonNull).toList());
        }
    }

    /** Nhãn hạng mục chỉ toàn khoảng trắng coi như không nhập, để không hiện ô trống trên phiếu. */
    private String normalizeCategoryLabel(String rawLabel) {
        if (rawLabel == null) return null;
        String label = rawLabel.trim();
        return label.isEmpty() ? null : label;
    }

    private List<EstimateItem> resolveItems(List<EstimateItemReqDto> itemRequests,
                                            Integer estimateId,
                                            Integer fallbackPricingConfigId,
                                            BigDecimal manualMarkupMultiplier,
                                            EstimateTypeEnum estimateType) {
        return itemRequests.stream().map(req -> {
            Integer categoryId = req.getItemCategoryId();
            // Chỉ tra danh mục có sẵn để lấy thuế mặc định. Tên nhóm gõ tay được giữ
            // nguyên dạng chữ trên dòng, không sinh thêm bản ghi danh mục dùng chung.
            ItemCategory itemCategory = categoryId != null ? itemCategoryRepo.findById(categoryId) : null;

            EstimateItem item = new EstimateItem();
            item.setEstimateId(estimateId);
            item.setItemCategoryId(categoryId);
            item.setCategoryLabel(normalizeCategoryLabel(req.getCategoryLabel()));
            item.setItemId(req.getItemId());
            item.setItemName(req.getItemName());
            item.setQuantity(req.getQuantity());
            validateLineQuantity(req);
            item.setSerialIdsJson(itemSerialService.toJson(req.getSerialIds()));

            // Recalculate unit price using config if provided, otherwise use request unit price
            BigDecimal calculatedUnitPrice = calculateMarkupUnitPrice(
                req.getItemId(),
                req.getWarehouseId(),
                fallbackPricingConfigId,
                manualMarkupMultiplier,
                estimateType,
                req.getUnitPrice()
            );
            item.setUnitPrice(calculatedUnitPrice);

            item.setWarehouseId(req.getWarehouseId());
            item.setUnit(req.getUnit());
            item.setIsChecked(req.getIsChecked() != null ? req.getIsChecked() : false);
            item.setRevisedFromItemId(req.getRevisedFromItemId());
            item.setTriggeredByItemId(req.getTriggeredByItemId());
            item.setPromotionId(req.getPromotionId());
            item.setEntryItemId(req.getEntryItemId());

            item.setIsGift(req.getIsGift() != null ? req.getIsGift() : false);
            applyOutsourceAndNote(item, req);
//            System.out.println("DEBUG RESOLVING ITEM: "+req.getIsGift()+", Tiggerd by: "+req.getRevisedFromItemId());
            TaxRule taxRule = null;
            Integer ruleId = null;
            //todo: check item taxt
            //check item have tax?
            if (req.getItemId()!=null){
                CatalogItemDto itemDto = warehouseInternalApi.getItemInfo(req.getItemId());
                if (itemDto != null && itemDto.getTaxRuleId() != null) {
                    ruleId = itemDto.getTaxRuleId();
                }
            }
            //if item tax null, check category tax
            if (ruleId == null && itemCategory != null && itemCategory.getTaxRuleId() != null) {
                ruleId = itemCategory.getTaxRuleId();
            }
            //if category tax null, check input tax
            if (ruleId == null && req.getTaxRuleId() != null) {
                ruleId = req.getTaxRuleId();
            }
            //todo: vat calculate
            BigDecimal quantity = Qty.nz(req.getQuantity());
            BigDecimal unitPrice = req.getUnitPrice();

            BigDecimal totalPrice = unitPrice.multiply(quantity);
            item.setTotalPrice(totalPrice);
            applyTax(item,ruleId);
            if (item.getIsGift()==true){
                item.setFinalPrice(BigDecimal.ZERO);
                item.setIsOverridden(false);
                item.setManualLineTotal(null);
            } else if (!applyManualLineTotal(item, req)) {
                item.setFinalPrice(item.getTotalPrice());
            }
            return item;
        }).toList();
    }

    /**
     * Nếu advisor khoá THÀNH TIỀN bằng tay thì dùng thẳng số đó cho dòng,
     * không tính lại theo SL x đơn giá x thuế. Trả về true nếu đã áp số gõ tay.
     */
    private boolean applyManualLineTotal(EstimateItem item, EstimateItemReqDto req) {
        boolean overridden = Boolean.TRUE.equals(req.getIsOverridden())
                && req.getManualLineTotal() != null
                && req.getManualLineTotal().compareTo(BigDecimal.ZERO) >= 0
                && !Boolean.TRUE.equals(item.getIsGift());
        if (overridden) {
            item.setIsOverridden(true);
            item.setManualLineTotal(req.getManualLineTotal());
            item.setTotalPrice(req.getManualLineTotal());
            item.setFinalPrice(req.getManualLineTotal());
        } else {
            item.setIsOverridden(false);
            item.setManualLineTotal(null);
        }
        return overridden;
    }
    /**
     * Tra tên các đối tác thuê ngoài đang được dùng trong danh sách dòng báo giá.
     * Gom một lượt để tránh gọi lặp lại theo từng dòng.
     */
    private Map<Integer, String> resolveOutsourcePartnerNames(List<EstimateItem> items) {
        List<Integer> partnerIds = items.stream()
                .map(EstimateItem::getOutsourcePartnerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (partnerIds.isEmpty()) return Map.of();

        List<CustomerProfile> partners = customerInternalApi.findAllByIds(partnerIds);
        if (partners == null) return Map.of();

        Map<Integer, String> result = new HashMap<>();
        for (CustomerProfile partner : partners) {
            if (partner != null && partner.getCustomerId() != null) {
                result.put(partner.getCustomerId(), partner.getFullName());
            }
        }
        return result;
    }

    private EstimateRespondDto getEstimateRespondDto(Integer estimateId) {
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        if (estimate == null) {
            throw new RuntimeException("Estimate not found");
        }
        List<EstimateItem> items = estimateItemRepository.findByEstimateId(estimateId).stream()
                .filter(i -> Boolean.FALSE.equals(i.getIsRemoved()))
                .toList();

        List<Integer> categoryIds = items.stream()
                .map(EstimateItem::getItemCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, ItemCategory> categoryMap = itemCategoryRepo
                .findAllById(categoryIds).stream()
                .collect(Collectors.toMap(ItemCategory::getId, wc -> wc));

        Map<Integer, String> outsourcePartnerNames = resolveOutsourcePartnerNames(items);

        EstimateRespondDto dto = estimateDtoMapper.toEstimateDto(estimate);
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal subTotal = BigDecimal.ZERO;
        List<EstimateItemDto> itemDtos = new ArrayList<>();

        for (EstimateItem item : items) {
            // MapStruct tự động lôi taxAmount, appliedTaxRate, totalPrice từ Entity sang DTO
            EstimateItemDto itemDto = estimateDtoMapper.toEstimateItemDto(item);

            // Populate import price for frontend calculation
            BigDecimal itemImportPrice = null;
            if (item.getEntryItemId() != null) {
                Optional<StockEntryItemJpa> entryItemOpt = stockEntryItemJpaRepo.findById(item.getEntryItemId());
                if (entryItemOpt.isPresent()) {
                    itemImportPrice = entryItemOpt.get().getImportPrice();
                }
            }
            if (itemImportPrice == null && item.getItemId() != null && item.getWarehouseId() != null) {
                List<StockEntryItemJpa> lots = stockEntryItemJpaRepo.findLatestLot(item.getWarehouseId(), item.getItemId());
                if (lots != null && !lots.isEmpty()) {
                    itemImportPrice = lots.get(0).getImportPrice();
                }
            }
            if (itemImportPrice == null && item.getItemId() != null) {
                CatalogItem catalogItem = warehouseInternalApi.findCatalogById(item.getItemId());
                if (catalogItem != null) {
                    itemImportPrice = catalogItem.getPrice();
                }
            }
            itemDto.setImportPrice(itemImportPrice != null ? itemImportPrice : BigDecimal.ZERO);

            // Map tên hạng mục
            ItemCategory wc = categoryMap.get(item.getItemCategoryId());
            if (wc != null) {
                itemDto.setItemCategory(estimateDtoMapper.toItemCateDto(wc));
            }

            if (item.getOutsourcePartnerId() != null) {
                itemDto.setOutsourcePartnerName(outsourcePartnerNames.get(item.getOutsourcePartnerId()));
            }

            // Cộng dồn tiền thuế
            if (item.getTaxAmount() != null) {
                totalTax = totalTax.add(item.getTaxAmount());
            }

            // Cộng dồn tiền gốc (Đơn giá * Số lượng)
            BigDecimal itemQty = Qty.nz(item.getQuantity());
            BigDecimal itemPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            subTotal = subTotal.add(itemPrice.multiply(itemQty));

            itemDtos.add(itemDto);
        }
        fillSerialCodes(itemDtos);
        dto.setItems(itemDtos);
        dto.setTotalTaxAmount(totalTax);
        dto.setSubTotal(subTotal);
        return dto;
    }

    @Transactional
    public EstimateItemReqDto updateEstimateItem(Integer estimateItemId, EstimateItemReqDto request) {
        EstimateItem estimateItem = estimateItemRepository.findByEstimateItemId(estimateItemId);

        Integer currentTaxRuleId = null;

        // Chọn danh mục thì gắn danh mục; bỏ chọn thì gỡ hẳn — dòng báo giá không
        // bắt buộc thuộc nhóm nào nữa.
        estimateItem.setItemCategoryId(request.getItemCategoryId());
        if (request.getCategoryLabel() != null) {
            estimateItem.setCategoryLabel(normalizeCategoryLabel(request.getCategoryLabel()));
        }
        if (request.getItemCategoryId() != null) {
            ItemCategory picked = itemCategoryRepo.findById(request.getItemCategoryId());
            if (picked != null) currentTaxRuleId = picked.getTaxRuleId();
        }
        if (request.getItemId() != null)estimateItem.setItemId(request.getItemId());
        if (request.getItemName() != null)estimateItem.setItemName(request.getItemName());
        if (request.getQuantity() != null) {
            Integer targetItemId = request.getItemId() != null ? request.getItemId() : estimateItem.getItemId();
            if (targetItemId != null) {
                itemQuantityPolicy.validateSaleQuantity(targetItemId, request.getQuantity(), estimateItem.getItemName());
            }
        }
        if (request.getSerialIds() != null) estimateItem.setSerialIdsJson(itemSerialService.toJson(request.getSerialIds()));
        if (request.getQuantity() != null)estimateItem.setQuantity(request.getQuantity());
        if (request.getUnitPrice() != null)estimateItem.setUnitPrice(request.getUnitPrice());
        if (request.getIsChecked() != null)estimateItem.setIsChecked(request.getIsChecked());
        if (request.getIsRemoved() != null)estimateItem.setIsRemoved(request.getIsRemoved());
        if (request.getWarehouseId() != null) estimateItem.setWarehouseId(request.getWarehouseId());
        estimateItem.setEntryItemId(request.getEntryItemId());

        BigDecimal quantity = Qty.nz(estimateItem.getQuantity());
        BigDecimal unitPrice = estimateItem.getUnitPrice() != null ? estimateItem.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalPriceVal = unitPrice.multiply(quantity);
        estimateItem.setTotalPrice(totalPriceVal);
        estimateItem.setTaxAmount(BigDecimal.ZERO);
        estimateItem.setAppliedTaxRate(BigDecimal.ZERO);

        Integer ruleId = currentTaxRuleId;
        if (ruleId == null) {
            if (estimateItem.getItemId() != null) {
                CatalogItemDto itemDto = warehouseInternalApi.getItemInfo(estimateItem.getItemId());
                if (itemDto != null && itemDto.getTaxRuleId() != null) {
                    ruleId = itemDto.getTaxRuleId();
                }
            }
            if (ruleId == null && estimateItem.getItemCategoryId() != null) {
                ItemCategory wc = itemCategoryRepo.findById(estimateItem.getItemCategoryId());
                if (wc != null && wc.getTaxRuleId() != null) {
                    ruleId = wc.getTaxRuleId();
                }
            }
            if (ruleId == null && request.getTaxRuleId() != null) {
                ruleId = request.getTaxRuleId();
            }
        }

        applyTax(estimateItem, ruleId);

        if (Boolean.TRUE.equals(estimateItem.getIsGift())) {
            estimateItem.setFinalPrice(BigDecimal.ZERO);
            estimateItem.setIsOverridden(false);
            estimateItem.setManualLineTotal(null);
        } else if (request.getIsOverridden() != null) {
            // Request nói rõ về việc khoá THÀNH TIỀN tay -> theo request
            if (!applyManualLineTotal(estimateItem, request)) {
                estimateItem.setFinalPrice(estimateItem.getTotalPrice());
            }
        } else if (Boolean.TRUE.equals(estimateItem.getIsOverridden()) && estimateItem.getManualLineTotal() != null) {
            // Request im lặng (vd huỷ giữ hàng) -> giữ nguyên số THÀNH TIỀN đã khoá tay trước đó
            estimateItem.setTotalPrice(estimateItem.getManualLineTotal());
            estimateItem.setFinalPrice(estimateItem.getManualLineTotal());
        } else {
            estimateItem.setFinalPrice(estimateItem.getTotalPrice());
        }

        EstimateItem saved = estimateItemRepository.save(estimateItem);
        //todo: recalculate
        syncSerialReservations(saved.getEstimateId());
        List<EstimateItem> allItems = estimateItemRepository.findByEstimateId(saved.getEstimateId());
        BigDecimal totalPrice = allItems.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsChecked()))
                .filter(i -> Boolean.FALSE.equals(i.getIsRemoved()))
                .map(i -> i.getFinalPrice() != null ? i.getFinalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Estimate estimate = estimateRepository.findEstimateById(saved.getEstimateId());
        estimate.setTotalPrice(totalPrice);
        estimateRepository.save(estimate);
        return estimateDtoMapper.toEstimateItemReqDto(saved);

    }
    private void applyTax(EstimateItem item,Integer taxRuleId) {
        BigDecimal quantity = Qty.nz(item.getQuantity());
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal subTotal = unitPrice.multiply(quantity);
        if (taxRuleId != null) {
            TaxRule taxRule = taxRuleRepository.findById(taxRuleId); // Tùy cách bạn viết repo, có thể bỏ .orElse(null)

            if (taxRule != null && taxRule.getTaxRate() != null) {
                BigDecimal taxRate = taxRule.getTaxRate();

                // a. Lưu cứng % thuế (Ví dụ: 8.0)
                item.setAppliedTaxRate(taxRate);

                // b. Lưu cứng Số tiền thuế (TaxAmount = SubTotal * TaxRate / 100)
                BigDecimal taxAmount = subTotal.multiply(taxRate).divide(BigDecimal.valueOf(100));
                item.setTaxAmount(taxAmount);

                // c. Tổng thanh toán = Tiền gốc + Tiền thuế
                item.setTotalPrice(subTotal.add(taxAmount));
                return;
            }
            item.setAppliedTaxRate(BigDecimal.ZERO);
            item.setTaxAmount(BigDecimal.ZERO);
            item.setTotalPrice(subTotal);
        }
    }

    public EstimateRespondDto updateEstimateApprove(Integer estimateId) {
        Estimate estimate =  estimateRepository.findEstimateById(estimateId);
        estimate.setStatus(EstimateEnum.APPROVED);
        Estimate saved = estimateRepository.save(estimate);
        return estimateDtoMapper.toEstimateDto(saved);
    }

    public EstimateRespondDto updateEstimateStatus(Integer estimateId, EstimateEnum status) {
        Estimate estimate =  estimateRepository.findEstimateById(estimateId);
        estimate.setStatus(status);
        Estimate saved = estimateRepository.save(estimate);
        return estimateDtoMapper.toEstimateDto(saved);
    }

    public List<ItemCateDto> getItemCateList() {
        List<ItemCategory> workCategories = itemCategoryRepo.findAll();
        return workCategories.stream().map(estimateDtoMapper::toItemCateDto).toList();
    }

    public Estimate findById(Integer estimateId) {
        return estimateRepository.findEstimateById(estimateId);
    }
    @Transactional
    public EstimateRespondDto applyPromotionToEstimate(Integer promotionId, Integer estimateId, String promotionCode) {
        Promotion promotion;
        if (promotionId != null){
            promotion = promotionInternalApi.findById(promotionId);
        }else
        if (promotionCode != null) {
            promotion = promotionInternalApi.findByPromotionCode(promotionCode);
        }else {
            throw new EstimateException("PROMOTION_404", EstimateErrorCode.PROMOTION_404);
        }
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        List<EstimateItem> items = estimateItemRepository.findByEstimateId(estimateId);
        if (promotion.getUsedCount()==null){
            System.err.println("USED COUNT NULL!");
            promotion.setUsedCount(0);
            promotionInternalApi.savePromotion(promotion);
        }
        validatePromotion(promotion,estimate,items);
        //todo: update estimateItems and estimate
        if (promotion.getType().equals("PERCENT")){
            //todo: apply %
            applyPercentPromotion(promotion, items);
        }else if (promotion.getType().equals("BUY_X_GET_Y")){
            //todo: apply buy x get y
            applyBuyXGetY(promotion, items, estimateId);
        }
        //todo: update total_price

        BigDecimal totalPrice = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsChecked()))
                .filter(item -> Boolean.FALSE.equals(item.getIsRemoved()))
                .map(item -> item.getFinalPrice() != null ? item.getFinalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        estimate.setTotalPrice(totalPrice);
        System.out.println("Total_price: " + totalPrice); // dùng biến totalPrice trực tiếp
        estimateRepository.save(estimate);

        //todo:update used race condition
        int rowsUpdated = promotionInternalApi.incrementUsedCountIfAvailable(promotionId);
        if (rowsUpdated==0) throw new EstimateException("Rất tiếc, giảm giá đã dùng hết rồi!", EstimateErrorCode.PROMOTION_OUT);
        return getEstimateRespondDto(estimateId);
    }

    private void applyBuyXGetY(Promotion promotion, List<EstimateItem> items, Integer estimateId) {
        List<PromotionBuyItem> buyItems = promotionInternalApi.findBuyItemsByPromotionId(promotion);
        List<PromotionGiftItem> giftItems = promotionInternalApi.findGiftItemsByPromotionId(promotion);
        int multiplier = computeGiftMultiplier(buyItems, items);
        if (multiplier < 1) {
            throw new EstimateException("Sản phẩm không đủ điều kiện khuyến mãi", EstimateErrorCode.PROMOTION_404);
        }
        EstimateItem triggerItem = items.stream()
                .filter(item -> item.getItemId().equals(buyItems.get(0).getCatalogItemId()))
                .findFirst()
                .orElseThrow();

        //todo:delete old gift items
        estimateRepository.deleteOldGitItemsByEstimateId(estimateId);

        for (PromotionGiftItem giftItemConfig : giftItems) {
            //find catalog that become gift
            CatalogItem catalogItem = warehouseInternalApi.findCatalogById(giftItemConfig.getCatalogItemId());
            int giftQuantity = giftItemConfig.getQuantity() * multiplier;

            EstimateItem giftItem = new EstimateItem();
            giftItem.setEstimateId(estimateId);
            giftItem.setItemId(giftItemConfig.getCatalogItemId());
            giftItem.setItemName(catalogItem.getItemName());
            giftItem.setQuantity(Qty.of(giftQuantity));
            //before promotion
            //find price in db

            giftItem.setTotalPrice(BigDecimal.ZERO);

            //after promotion
            giftItem.setDiscountAmount(giftItem.getTotalPrice());
            giftItem.setFinalPrice(BigDecimal.ZERO);
            giftItem.setPromotionId(promotion.getPromotionId());
            giftItem.setIsGift(Boolean.TRUE);
            giftItem.setIsChecked(Boolean.TRUE);
            giftItem.setUnit(triggerItem.getUnit());
            giftItem.setTriggeredByItemId(triggerItem.getItemId());
            //todo: find FREE workCate if Catalog have no W
            if (catalogItem.getItemCategoryId()==null||catalogItem.getItemCategoryId()==0){
                throw new EstimateException("Danh mục không được tạo với phân loại phù hợp (itemCategory_404)", EstimateErrorCode.BAD_DATA);
            }
            giftItem.setItemCategoryId(catalogItem.getItemCategoryId());
            //todo: check warehouse quantity available
            Integer warehouseId = resolveGiftItemWarehouse(giftItem,triggerItem);
            giftItem.setWarehouseId(warehouseId);
            BigDecimal unitPrice = warehouseInternalApi.findItemPricing(catalogItem.getItemId(),warehouseId!= null ? warehouseId : triggerItem.getWarehouseId(),catalogItem.getPrice());
            giftItem.setUnitPrice(unitPrice!=null?unitPrice:BigDecimal.ZERO);

            estimateItemRepository.save(giftItem);
        }
    }

    /**
     * Nhóm mua là điều kiện AND: phải mua đủ TẤT CẢ item trong nhóm mua.
     * Hệ số nhân quà tặng = số lần tối đa combo mua được lặp lại, tức min(floor(soLuongMua_i / soLuongYeuCau_i)).
     */
    private int computeGiftMultiplier(List<PromotionBuyItem> buyItems, List<EstimateItem> items) {
        if (buyItems.isEmpty()) {
            return 0;
        }
        int multiplier = Integer.MAX_VALUE;
        for (PromotionBuyItem buyItem : buyItems) {
            BigDecimal purchasedQuantity = Qty.sum(items.stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getIsGift()))
                    .filter(item -> item.getItemId().equals(buyItem.getCatalogItemId()))
                    .toList(), EstimateItem::getQuantity);
            int ratio = buyItem.getQuantity() == null || buyItem.getQuantity() <= 0 ? 0
                    : purchasedQuantity.divideToIntegralValue(Qty.of(buyItem.getQuantity())).intValue();
            multiplier = Math.min(multiplier, ratio);
        }
        return multiplier == Integer.MAX_VALUE ? 0 : multiplier;
    }

    private Integer resolveGiftItemWarehouse(EstimateItem giftItem, EstimateItem triggerItem) {
        //find by triggerItem warehouse
        if (triggerItem.getWarehouseId()!=null){
            Inventory inventory = warehouseInternalApi.findInventoryByWarehouseIdAndItemIds(triggerItem.getWarehouseId(),giftItem.getItemId());
            if (inventory!=null&&Qty.isPositive(inventory.getAvailableQuantity())){
                return triggerItem.getWarehouseId();
            }
        }
        //find another warehouse
        Inventory fallback = warehouseInternalApi.findItemAvailableInOtherWarehouse(giftItem.getItemId(),0);
        if (fallback!=null&&Qty.isPositive(fallback.getAvailableQuantity())){
            return fallback.getItemId();
        }
        // not available
        return null;

    }

    private void applyPercentPromotion(Promotion promotion, List<EstimateItem> items) {
        List<EstimateItem> targetItems;

        if (promotion.getApplyTo().equals("ALL")){
            targetItems = items.stream()
            .filter(item -> !item.getIsGift())
            .toList();
        }else {
            List<Integer> eligibleItemIds = promotionInternalApi
            .findItemIdsByPromotionId(promotion);
        targetItems = items.stream()
            .filter(item -> eligibleItemIds.contains(item.getItemId()))
            .toList();
        }

        //todo:update each items
        targetItems.forEach(estimateItem -> {
            BigDecimal discount = estimateItem.getSubTotal()
                    .multiply(promotion.getDiscountPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            estimateItem.setDiscountAmount(discount);
            BigDecimal quantity = Qty.nz(estimateItem.getQuantity());
            BigDecimal newTotalPrice = estimateItem.getUnitPrice().multiply(quantity);
            BigDecimal taxRate = estimateItem.getAppliedTaxRate()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal finalPrice = newTotalPrice
                    .subtract(discount)
                    .multiply(BigDecimal.ONE.add(taxRate))
                    .setScale(2, RoundingMode.HALF_UP);
            estimateItem.setFinalPrice(finalPrice);
            estimateItem.setPromotionId(promotion.getPromotionId());
        });
        estimateItemRepository.saveAll(targetItems);
    }

    private void validatePromotion(Promotion promotion, Estimate estimate, List<EstimateItem> items) {
        if (promotion == null) {
            throw new EstimateException("Mã giảm giá không khả dụng", EstimateErrorCode.PROMOTION_404);
        }
        if (promotion.getIsActive().equals(Boolean.FALSE)) {
            throw new EstimateException("Mã giảm giá không khả dụng", EstimateErrorCode.PROMOTION_404);
        }
        if (promotion.getEndDate().isBefore(LocalDate.now())){
            throw new EstimateException("Mã giảm giá hết hạn", EstimateErrorCode.PROMOTION_404);
        }
        if (promotion.getUsageLimit() != null && promotion.getUsedCount()>=promotion.getUsageLimit()){
            throw new EstimateException("Mã giảm giá đã dùng hết", EstimateErrorCode.PROMOTION_404);
        }
        if (promotion.getMinOrderValue() != null && promotion.getMinOrderValue().compareTo(estimate.getTotalPrice()) > 0){
            throw new EstimateException("Giá trị đơn hàng không đủ", EstimateErrorCode.PROMOTION_404);
        }
        if (promotion.getType().equals("PERCENT")&&promotion.getApplyTo().equals("SPECIFIC")){
            List<Integer> promotionItems = promotionInternalApi.findItemIdsByPromotionId(promotion);
            boolean hasMatchItems = items.stream().anyMatch(item -> promotionItems.contains(item.getItemId()));
            if (!hasMatchItems) {
                throw new EstimateException("Không tìm thấy sản phẩm đủ điều kiện giảm giá", EstimateErrorCode.PROMOTION_404);
            }
        }
        if (promotion.getType().equals("BUY_X_GET_Y")){
            List<PromotionBuyItem> buyItems = promotionInternalApi.findBuyItemsByPromotionId(promotion);
            if (computeGiftMultiplier(buyItems, items) < 1) {
                throw new EstimateException("Sản phẩm không đủ điều kiện khuyến mãi", EstimateErrorCode.PROMOTION_404);
            }
        }

    }

    public EstimateRespondDto unapplyPromotionToEstimate(Integer promotionId, Integer estimateId, String promotionCode) {
        Promotion promotion;
        if (promotionId != null){
            promotion = promotionInternalApi.findById(promotionId);
        }else
        if (promotionCode != null) {
            promotion = promotionInternalApi.findByPromotionCode(promotionCode);
        }else {
            throw new EstimateException("PROMOTION_404", EstimateErrorCode.PROMOTION_404);
        }
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        if (estimate.getStatus().equals(EstimateEnum.ARCHIVED)){
            throw new EstimateException("ESTIMATE_ARCHIVED", EstimateErrorCode.BAD_REQUEST);
        }

        List<EstimateItem> items = estimateItemRepository.findByEstimateId(estimateId);


        //todo: update estimateItems and estimate
        if (promotion.getType().equals("PERCENT")){
            //todo: apply %
            unapplyPercentPromotion(promotion, items);
        }else if (promotion.getType().equals("BUY_X_GET_Y")){
            //todo: apply buy x get y
            unapplyBuyXGetY(promotion, items, estimateId);
        }
        if (promotion.getUsedCount()==null||promotion.getUsedCount()==0){
            promotion.setUsedCount(0);
        }else promotion.setUsedCount(promotion.getUsedCount()-1);
        promotionInternalApi.savePromotion(promotion);
        BigDecimal newTotalPrice = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsChecked()))
                .filter(item -> Boolean.FALSE.equals(item.getIsRemoved()))
                .map(item -> item.getFinalPrice() != null ? item.getFinalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        estimate.setTotalPrice(newTotalPrice);
        estimateRepository.save(estimate);
        return getEstimateRespondDto(estimateId);
    }

    private void unapplyBuyXGetY(Promotion promotion, List<EstimateItem> items, Integer estimateId) {
        List<EstimateItem> giftItems = items.stream()
        .filter(item -> Boolean.TRUE.equals(item.getIsGift())
                && promotion.getPromotionId().equals(item.getPromotionId()))
        .toList();
    estimateItemRepository.deleteAll(giftItems);
    }

    private void unapplyPercentPromotion(Promotion promotion, List<EstimateItem> items) {
        List<EstimateItem> affectedItems = items.stream().filter(estimateItem -> promotion.getPromotionId().equals(estimateItem.getPromotionId())).toList();
        affectedItems.forEach(estimateItem -> {
            estimateItem.setDiscountAmount(null);
            estimateItem.setPromotionId(null);

            BigDecimal quantity = Qty.nz(estimateItem.getQuantity());
            BigDecimal unitPrice = estimateItem.getUnitPrice() != null ? estimateItem.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal basePrice = unitPrice.multiply(quantity);

            estimateItem.setTotalPrice(basePrice);

            if (estimateItem.getAppliedTaxRate()!=null){
                BigDecimal tax = basePrice
                        .multiply(estimateItem.getAppliedTaxRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                estimateItem.setTaxAmount(tax);
                estimateItem.setFinalPrice(basePrice.add(tax));
            }else {
                estimateItem.setTaxAmount(BigDecimal.ZERO);
                estimateItem.setFinalPrice(basePrice);
            }

        });
        estimateItemRepository.saveAll(affectedItems);
    }

    public EstimateRespondDto getEstimateByBookingId(Integer bookingId) {
        Integer estimateId = bookingManageInternalApi.findEstimateId(bookingId);
        if (estimateId == null) {
            System.err.println("EstimateId is null");
            return null;
        }

        // 1. Tìm estimate
        Estimate estimate = estimateRepository.findEstimateById(estimateId);

        // EARLY RETURN
        if (estimate == null) {
            return null;
        }

        // 2. Lấy estimate items của estimate này và lọc bỏ đồ bị xóa
        List<EstimateItem> estimateItems = estimateItemRepository.findByEstimateIds(List.of(estimateId))
                .stream()
                .filter(item -> Boolean.FALSE.equals(item.getIsRemoved()))
                .toList();

        // 3. Chuẩn bị các ID cần thiết để query
        List<Integer> warehouseIds = estimateItems.stream()
                .map(EstimateItem::getWarehouseId)
                .filter(Objects::nonNull) // Đã mở lại để tránh lỗi null
                .distinct()
                .toList();

        List<Integer> itemCategoryIds = estimateItems.stream()
                .map(EstimateItem::getItemCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 4. Lấy dữ liệu Map (Dùng toán tử 3 ngôi cho gọn)
        Map<Integer, ItemCategory> categoryMap = itemCategoryIds.isEmpty() ? new HashMap<>() :
                itemCategoryRepo.findAllById(itemCategoryIds).stream()
                        .collect(Collectors.toMap(ItemCategory::getId, wc -> wc));

        Map<Integer, Warehouse> warehouseMap = warehouseIds.isEmpty() ? new HashMap<>() :
                warehouseInternalApi.findAllById(warehouseIds).stream()
                        .collect(Collectors.toMap(Warehouse::getWarehouseId, w -> w));

        Map<Integer, StockAllocation> allocationMap = estimateItems.isEmpty() ? new HashMap<>() :
                stockAllocationRepository.findAllByEstimateId(estimateItems).stream()
                        .collect(Collectors.toMap(StockAllocation::getEstimateItemId, a -> a));

        // 5. Map dữ liệu sang DTO (Chỉ xử lý 1 Estimate, không cần vòng lặp stream() bọc ngoài)
        EstimateRespondDto dto = estimateDtoMapper.toEstimateDto(estimate);
        BigDecimal oldPrice = dto.getTotalPrice() != null ? dto.getTotalPrice() : BigDecimal.ZERO;

        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal finalPrice = BigDecimal.ZERO;
        List<EstimateItemDto> itemDtos = new ArrayList<>();
        Set<Integer> promotionIds = new HashSet<>();

        for (EstimateItem item : estimateItems) {
            EstimateItemDto itemDto = estimateDtoMapper.toEstimateItemDto(item);

            // Inject work category
            if (item.getItemCategoryId() != null) {
                ItemCategory wc = categoryMap.get(item.getItemCategoryId());
                if (wc != null) {
                    itemDto.setItemCategory(estimateDtoMapper.toItemCateDto(wc));
                }
            }

            // Inject warehouse
            if (item.getWarehouseId() != null) {
                Warehouse wh = warehouseMap.get(item.getWarehouseId());
                if (wh != null) {
                    itemDto.setWarehouse(warehouseDtoMapper.toDtoInternal(wh));
                }
            }

            // Inject stock allocation & return status
            if (item.getId() != null) {
                StockAllocation allocation = allocationMap.get(item.getId());
                if (allocation != null) {
                    Pair<Integer, String> returnPair = warehouseInternalApi.getReturnStatusByAlloId(allocation.getAllocationId());
                    StockAllocationDto allocationDto = stockAllocationDtoMapper.toDto(allocation);

                    if (returnPair != null) {
                        allocationDto.setReturnId(returnPair.getLeft());
                        allocationDto.setReturnStatus(returnPair.getRight());
                    }
                    itemDto.setStockAllocation(allocationDto);
                }
            }

            // Tính toán giá tiền cho các item được checked
            if (Boolean.TRUE.equals(item.getIsChecked())) {
                if (item.getTaxAmount() != null) {
                    totalTax = totalTax.add(item.getTaxAmount());
                }

                BigDecimal itemQty = Qty.nz(item.getQuantity());
                BigDecimal itemPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                subTotal = subTotal.add(itemPrice.multiply(itemQty));

                BigDecimal itemFinalPrice = item.getFinalPrice() != null ? item.getFinalPrice() : BigDecimal.ZERO;
                finalPrice = finalPrice.add(itemFinalPrice);

                if (item.getPromotionId() != null) {
                    promotionIds.add(item.getPromotionId());
                }
            }
            itemDtos.add(itemDto);
        }

        // Set lại thông tin cho DTO
        dto.setTotalPrice(finalPrice);
        dto.setSubTotal(subTotal);
        dto.setTotalTaxAmount(totalTax);
        fillSerialCodes(itemDtos);
        dto.setItems(itemDtos);
        dto.setPromotions(new ArrayList<>(promotionIds));

        // Cập nhật giá mới nếu có thay đổi
        if (oldPrice.compareTo(finalPrice) != 0) {
            // Tận dụng luôn object `estimate` đã lấy từ đầu, không cần query lại findEstimateById
            estimate.setTotalPrice(finalPrice);
            estimateRepository.save(estimate);
        }

        return dto;
    }

    private BigDecimal calculateMarkupUnitPrice(Integer itemId, Integer warehouseId, Integer configId, EstimateTypeEnum type, BigDecimal defaultPrice) {
        return calculateMarkupUnitPrice(itemId, warehouseId, configId, null, type, defaultPrice);
    }

    /**
     * Đơn giá = giá vốn × hệ số markup.
     *
     * Hệ số lấy theo thứ tự: hệ số gõ tay của phiếu → cấu hình markup được chọn.
     * Không có hệ số nào, hoặc không tra được giá vốn, thì giữ nguyên đơn giá gửi lên.
     */
    private BigDecimal calculateMarkupUnitPrice(Integer itemId,
                                                Integer warehouseId,
                                                Integer configId,
                                                BigDecimal manualMarkupMultiplier,
                                                EstimateTypeEnum type,
                                                BigDecimal defaultPrice) {
        boolean hasManualMarkup = manualMarkupMultiplier != null
                && manualMarkupMultiplier.compareTo(BigDecimal.ZERO) > 0;
        if (itemId == null || (configId == null && !hasManualMarkup)) {
            return defaultPrice != null ? defaultPrice : BigDecimal.ZERO;
        }
        try {
            FallbackPricingConfigJpa config = null;
            if (!hasManualMarkup) {
                Optional<FallbackPricingConfigJpa> configOpt = fallbackPricingConfigJpaRepo.findById(configId);
                if (configOpt.isEmpty() || !Boolean.TRUE.equals(configOpt.get().getIsActive())) {
                    return defaultPrice != null ? defaultPrice : BigDecimal.ZERO;
                }
                config = configOpt.get();
            }

            // Find cost (importPrice)
            BigDecimal cost = null;
            if (warehouseId != null) {
                List<StockEntryItemJpa> lots = stockEntryItemJpaRepo.findLatestLot(warehouseId, itemId);
                if (lots != null && !lots.isEmpty()) {
                    cost = lots.get(0).getImportPrice();
                }
            }
            if (cost == null) {
                CatalogItem catalogItem = warehouseInternalApi.findCatalogById(itemId);
                cost = catalogItem != null ? catalogItem.getPrice() : null;
            }
            // Không tra được giá vốn hợp lệ (không có lô hàng, catalog chưa có giá...) thì giữ nguyên
            // đơn giá đã gửi lên thay vì nhân với 0, tránh đánh mất đơn giá/thành tiền của dòng ước tính.
            if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
                return defaultPrice != null ? defaultPrice : BigDecimal.ZERO;
            }

            BigDecimal multiplier = hasManualMarkup ? manualMarkupMultiplier : config.getMarkupMultiplier();
            if (multiplier == null) {
                multiplier = BigDecimal.ONE;
            }

            return cost.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultPrice != null ? defaultPrice : BigDecimal.ZERO;
        }
    }

    @Transactional
    public EstimateRespondDto applyFallbackPricingToEstimate(Integer estimateId, Integer configId) {
        return applyFallbackPricingToEstimate(estimateId, configId, null);
    }

    /**
     * Áp lại markup cho toàn phiếu.
     * Truyền manualMarkupMultiplier khi cố vấn gõ thẳng hệ số thay vì chọn cấu hình;
     * lúc đó configId được xoá để tránh hai nguồn hệ số cùng tồn tại trên một phiếu.
     */
    @Transactional
    public EstimateRespondDto applyFallbackPricingToEstimate(Integer estimateId,
                                                             Integer configId,
                                                             BigDecimal manualMarkupMultiplier) {
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        if (estimate == null) {
            throw new RuntimeException("Estimate not found");
        }
        boolean hasManualMarkup = manualMarkupMultiplier != null
                && manualMarkupMultiplier.compareTo(BigDecimal.ZERO) > 0;
        estimate.setFallbackPricingConfigId(hasManualMarkup ? null : configId);
        estimate.setManualMarkupMultiplier(hasManualMarkup ? manualMarkupMultiplier : null);
        estimateRepository.save(estimate);

        List<EstimateItem> items = estimateItemRepository.findByEstimateId(estimateId);
        for (EstimateItem item : items) {
            if (Boolean.TRUE.equals(item.getIsRemoved()) || Boolean.TRUE.equals(item.getIsGift())) {
                continue;
            }
            if (item.getItemId() != null) {
                BigDecimal originalPrice = item.getUnitPrice();
                BigDecimal nextPrice = calculateMarkupUnitPrice(
                        item.getItemId(),
                        item.getWarehouseId(),
                        estimate.getFallbackPricingConfigId(),
                        estimate.getManualMarkupMultiplier(),
                        estimate.getEstimateType(),
                        originalPrice);
                item.setUnitPrice(nextPrice);
                
                // Recalculate subtotal and tax based on existing appliedTaxRate
                BigDecimal quantity = Qty.nz(item.getQuantity());
                BigDecimal subTotal = nextPrice.multiply(quantity);
                BigDecimal taxRate = item.getAppliedTaxRate();
                if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal taxAmount = subTotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    item.setTaxAmount(taxAmount);
                    item.setTotalPrice(subTotal.add(taxAmount));
                } else {
                    item.setTaxAmount(BigDecimal.ZERO);
                    item.setTotalPrice(subTotal);
                }
                item.setFinalPrice(item.getTotalPrice());
                
                estimateItemRepository.save(item);
            }
        }
        return getEstimateRespondDto(estimateId);
    }
}
