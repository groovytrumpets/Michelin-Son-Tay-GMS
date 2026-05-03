package com.g42.platform.gms.estimation.app.service;

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
import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.api.internal.WarehouseInternalApi;
import com.g42.platform.gms.warehouse.api.mapper.WarehouseDtoMapper;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
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
    private final WorkCategoryRepository workCategoryRepo;
    private final EstimateDtoMapper estimateDtoMapper;
    private final TaxRuleRepository taxRuleRepository;

    private final WarehouseInternalApi warehouseInternalApi;
    private final TaxRuleInternalApi taxRuleInternalApi;
    private final WarehouseDtoMapper warehouseDtoMapper;

    private final PromotionInternalApi promotionInternalApi;
    private final StockAllocationRepository stockAllocationRepository;
    private final StockAllocationDtoMapper stockAllocationDtoMapper;


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
        List<Integer> workCategoryIds = estimateItems.stream()
                .map(EstimateItem::getWorkCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Khởi tạo map rỗng, chỉ query DB nếu có workCategoryIds
        Map<Integer, WorkCategory> categoryMap;
        if (!workCategoryIds.isEmpty()) {
            categoryMap = workCategoryRepo.findAllById(workCategoryIds)
                    .stream()
                    .collect(Collectors.toMap(WorkCategory::getId, wc -> wc));
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
                if (item.getWorkCategoryId() != null) {
                    WorkCategory wc = categoryMap.get(item.getWorkCategoryId());
                    if (wc != null) {
                        itemDto.setWorkCategory(estimateDtoMapper.toWorkCateDto(wc));
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
                    BigDecimal itemQty = BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 0);
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
        Estimate saved = estimateRepository.save(estimate);

        List<EstimateItem> items = resolveItems(request.getItems(), saved.getId());
        estimateItemRepository.saveAll(items);

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

    public EstimateRespondDto updateEstimate(Integer estimateId, EstimateRequestDto request) {
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        if (estimate == null) throw new RuntimeException("Estimate not found");

        List<EstimateItem> estimateItems = estimateItemRepository.findByEstimateId(estimateId);
        Map<Integer, EstimateItem> existingMap = estimateItems.stream()
                .filter(i -> i.getId() != null)
                .collect(Collectors.toMap(EstimateItem::getId, i -> i));
        List<EstimateItem> toSave = new ArrayList<>();
        Set<Integer> incomingIds = new HashSet<>();
        for (EstimateItemReqDto req : request.getItems()) {
            if (req.getItemId() != null && existingMap.containsKey(req.getItemId())) {
                // update old items
                EstimateItem existing = existingMap.get(req.getItemId());
                existing.setItemName(req.getItemName());
                existing.setQuantity(req.getQuantity());
                existing.setUnitPrice(req.getUnitPrice());
                existing.setWorkCategoryId(req.getWorkCategoryId());
                existing.setIsChecked(req.getIsChecked());
                existing.setIsRemoved(req.getIsRemoved());
                existing.setIsGift(req.getIsGift());
                existing.setTriggeredByItemId(req.getTriggeredByItemId());
                existing.setDiscountAmount(req.getDiscountAmount());
                WorkCategory wc = workCategoryRepo.findById(req.getWorkCategoryId());
                TaxRule taxRule = (wc != null) ? taxRuleRepository.findById(wc.getTaxRuleId()) : null;
                Integer taxRuleId = (wc != null && wc.getTaxRuleId() != null)
                        ? wc.getTaxRuleId()
                        : null;
                if (taxRule != null) {
                applyTax(existing,taxRuleId);
                }
                existing.setFinalPrice(existing.getFinalPrice());
                toSave.add(existing);
                incomingIds.add(req.getItemId());
            } else {
                toSave.addAll(resolveItems(List.of(req), estimateId));
            }
        }
        estimateItems.stream()
                .filter(i -> !incomingIds.contains(i.getId()))
                .forEach(i -> {
                    i.setIsRemoved(true);
                    estimateItemRepository.save(i);
                });

        estimateItemRepository.saveAll(toSave);
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
    private List<EstimateItem> resolveItems(List<EstimateItemReqDto> itemRequests,
                                            Integer estimateId) {
        return itemRequests.stream().map(req -> {
            Integer categoryId = req.getWorkCategoryId();
            WorkCategory workCategory = null;

            if (categoryId == null && req.getNewCategoryName() != null) {
            System.out.println("DEBUG CREATING CATA: ");
                WorkCategory newCategory = new WorkCategory();
                newCategory.setCategoryName(req.getNewCategoryName());
                newCategory.setCategoryCode(
                        req.getNewCategoryName().toUpperCase().replace(" ", "_")
                );
                newCategory.setIsDefault(false);
                newCategory.setIsActive(true);
                Integer finalTaxId = req.getTaxRuleId();
                if (finalTaxId == null) {
                    finalTaxId = taxRuleInternalApi.getTaxCodeFreeId("FREE");
                    if (finalTaxId==-1) finalTaxId=taxRuleInternalApi.createNewFreeTax();
                }
                newCategory.setTaxRuleId(finalTaxId);

                int nextOrder = workCategoryRepo.findMaxDisplayOrder()+1;
                newCategory.setDisplayOrder(nextOrder);
                workCategory= workCategoryRepo.save(newCategory);
                categoryId = workCategory.getId();
            }else if (categoryId != null) {
                // Nếu có categoryId, bốc từ DB lên để tý lấy ID thuế của nó
                workCategory = workCategoryRepo.findById(categoryId);
            }

            EstimateItem item = new EstimateItem();
            item.setEstimateId(estimateId);
            item.setWorkCategoryId(categoryId);
            item.setItemId(req.getItemId());
            item.setItemName(req.getItemName());
            item.setQuantity(req.getQuantity());
            item.setUnitPrice(req.getUnitPrice());
            item.setWarehouseId(req.getWarehouseId());
            item.setUnit(req.getUnit());
            item.setIsChecked(req.getIsChecked() != null ? req.getIsChecked() : false);
            item.setRevisedFromItemId(req.getRevisedFromItemId());
            item.setTriggeredByItemId(req.getTriggeredByItemId());
            item.setPromotionId(req.getPromotionId());

            item.setIsGift(req.getIsGift() != null ? req.getIsGift() : false);
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
            if (ruleId == null && workCategory != null && workCategory.getTaxRuleId() != null) {
                ruleId = workCategory.getTaxRuleId();
            }
            //if category tax null, check input tax
            if (ruleId == null && req.getTaxRuleId() != null) {
                ruleId = req.getTaxRuleId();
            }
            //todo: vat calculate
            BigDecimal quantity = BigDecimal.valueOf(req.getQuantity());
            BigDecimal unitPrice = req.getUnitPrice();

            BigDecimal totalPrice = unitPrice.multiply(quantity);
            item.setTotalPrice(totalPrice);
            applyTax(item,ruleId);
            if (item.getIsGift()==true){
                item.setFinalPrice(BigDecimal.ZERO);
            }else
            item.setFinalPrice(item.getTotalPrice());
            return item;
        }).toList();
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
                .map(EstimateItem::getWorkCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, WorkCategory> categoryMap = workCategoryRepo
                .findAllById(categoryIds).stream()
                .collect(Collectors.toMap(WorkCategory::getId, wc -> wc));


        EstimateRespondDto dto = estimateDtoMapper.toEstimateDto(estimate);
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal subTotal = BigDecimal.ZERO;
        List<EstimateItemDto> itemDtos = new ArrayList<>();

        for (EstimateItem item : items) {
            // MapStruct tự động lôi taxAmount, appliedTaxRate, totalPrice từ Entity sang DTO
            EstimateItemDto itemDto = estimateDtoMapper.toEstimateItemDto(item);

            // Map tên hạng mục
            WorkCategory wc = categoryMap.get(item.getWorkCategoryId());
            if (wc != null) {
                itemDto.setWorkCategory(estimateDtoMapper.toWorkCateDto(wc));
            }

            // Cộng dồn tiền thuế
            if (item.getTaxAmount() != null) {
                totalTax = totalTax.add(item.getTaxAmount());
            }

            // Cộng dồn tiền gốc (Đơn giá * Số lượng)
            BigDecimal itemQty = BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 0);
            BigDecimal itemPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            subTotal = subTotal.add(itemPrice.multiply(itemQty));

            itemDtos.add(itemDto);
        }
        dto.setItems(itemDtos);
        dto.setTotalTaxAmount(totalTax);
        dto.setSubTotal(subTotal);
        return dto;
    }

    public EstimateItemReqDto updateEstimateItem(Integer estimateItemId, EstimateItemReqDto request) {
        EstimateItem estimateItem = estimateItemRepository.findByEstimateItemId(estimateItemId);

        Integer currentTaxRuleId = null;

        if (request.getWorkCategoryId() != null) {
            estimateItem.setWorkCategoryId(request.getWorkCategoryId());
        } else if (request.getNewCategoryName() != null) {
            WorkCategory newCategory = new WorkCategory();
            newCategory.setCategoryName(request.getNewCategoryName());
            newCategory.setCategoryCode(
                    request.getNewCategoryName().toUpperCase().replace(" ", "_")
            );
            newCategory.setIsDefault(false);
            newCategory.setIsActive(true);
            System.out.println("DEBUG CREATING CATA");
            Integer finalTaxId = request.getTaxRuleId();
            if (finalTaxId == null){
                finalTaxId = taxRuleInternalApi.getTaxCodeFreeId("FREE");
                if (finalTaxId==null) {
                    finalTaxId = taxRuleInternalApi.createNewFreeTax();

                }
            }
            newCategory.setTaxRuleId(finalTaxId);
            int nextOrder = workCategoryRepo.findMaxDisplayOrder() + 1;
            newCategory.setDisplayOrder(nextOrder);
            WorkCategory saved = workCategoryRepo.save(newCategory);

            estimateItem.setWorkCategoryId(saved.getId());
            currentTaxRuleId = saved.getTaxRuleId();
        }
        //todo: handle newCate
        if (request.getItemId() != null)estimateItem.setItemId(request.getItemId());
        if (request.getItemName() != null)estimateItem.setItemName(request.getItemName());
        if (request.getQuantity() != null)estimateItem.setQuantity(request.getQuantity());
        if (request.getUnitPrice() != null)estimateItem.setUnitPrice(request.getUnitPrice());
        if (request.getIsChecked() != null)estimateItem.setIsChecked(request.getIsChecked());
        if (request.getIsRemoved() != null)estimateItem.setIsRemoved(request.getIsRemoved());
        applyTax(estimateItem,currentTaxRuleId);
        EstimateItem saved = estimateItemRepository.save(estimateItem);
        //todo: recalculate
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
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 0);
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

    public List<WorkCataDto> getWorkCateList() {
        List<WorkCategory> workCategories = workCategoryRepo.findAll();
        return workCategories.stream().map(estimateDtoMapper::toWorkCateDto).toList();
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
        if (rowsUpdated==0) throw new EstimateException("UNFORTUNATE, LAST PROMOTION IS USED", EstimateErrorCode.PROMOTION_OUT);
        return getEstimateRespondDto(estimateId);
    }

    private void applyBuyXGetY(Promotion promotion, List<EstimateItem> items, Integer estimateId) {
        EstimateItem triggerItem = items.stream()
        .filter(item -> item.getItemId().equals(promotion.getBuyItemId()))
        .findFirst()
        .orElseThrow();
        //count gift quantity
        int giftQuantity = (triggerItem.getQuantity()/promotion.getBuyQuantity())*promotion.getGetQuantity();

        //todo:delete old gift items
        estimateRepository.deleteOldGitItemsByEstimateId(estimateId);

        //find catalog that become gift
        CatalogItem catalogItem = warehouseInternalApi.findCatalogById(promotion.getGetItemId());

        EstimateItem giftItem = new EstimateItem();
        giftItem.setEstimateId(estimateId);
        giftItem.setItemId(promotion.getGetItemId());
        giftItem.setItemName(catalogItem.getItemName());
        giftItem.setQuantity(giftQuantity);
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
        if (catalogItem.getWorkCategoryId()==null||catalogItem.getWorkCategoryId()==0){
            throw new EstimateException("Danh mục không được tạo với phân loại phù hợp (workCategory_404)", EstimateErrorCode.BAD_DATA);
        }
        giftItem.setWorkCategoryId(catalogItem.getWorkCategoryId());
        //todo: check warehouse quantity available
        Integer warehouseId = resolveGiftItemWarehouse(giftItem,triggerItem);
        giftItem.setWarehouseId(warehouseId);
        BigDecimal unitPrice = warehouseInternalApi.findItemPricing(catalogItem.getItemId(),warehouseId!= null ? warehouseId : triggerItem.getWarehouseId(),catalogItem.getPrice());
        giftItem.setUnitPrice(unitPrice!=null?unitPrice:BigDecimal.ZERO);

        estimateItemRepository.save(giftItem);
    }

    private Integer resolveGiftItemWarehouse(EstimateItem giftItem, EstimateItem triggerItem) {
        //find by triggerItem warehouse
        if (triggerItem.getWarehouseId()!=null){
            Inventory inventory = warehouseInternalApi.findInventoryByWarehouseIdAndItemIds(triggerItem.getWarehouseId(),giftItem.getItemId());
            if (inventory!=null&&inventory.getAvailableQuantity()>0){
                return triggerItem.getWarehouseId();
            }
        }
        //find another warehouse
        Inventory fallback = warehouseInternalApi.findItemAvailableInOtherWarehouse(giftItem.getItemId(),0);
        if (fallback!=null&&fallback.getAvailableQuantity()>0){
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
            BigDecimal quantity = BigDecimal.valueOf(estimateItem.getQuantity());
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
            boolean hasMatchItems = items.stream().anyMatch(estimateItem ->
                    estimateItem.getItemId().equals(promotion.getBuyItemId())
                            &&estimateItem.getQuantity().equals(promotion.getBuyQuantity()));
            if (!hasMatchItems) {
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
            estimateItem.setFinalPrice(estimateItem.getTotalPrice());
            estimateItem.setPromotionId(null);
            if (estimateItem.getAppliedTaxRate()!=null){
                BigDecimal tax = estimateItem.getFinalPrice()
                        .multiply(estimateItem.getAppliedTaxRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                estimateItem.setTaxAmount(tax);
            }

        });
        estimateItemRepository.saveAll(affectedItems);
    }
}
