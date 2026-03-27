package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.estimation.api.dto.EstimateItemDto;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.estimation.api.dto.request.EstimateItemReqDto;
import com.g42.platform.gms.estimation.api.dto.request.EstimateRequestDto;
import com.g42.platform.gms.estimation.api.mapper.EstimateDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.TaxRule;
import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.estimation.domain.repository.TaxRuleRepository;
import com.g42.platform.gms.estimation.domain.repository.WorkCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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


    public List<EstimateRespondDto> getEstimateByCode(Integer serviceTicketId) {
        //todo: find all estimate
        List<Estimate> estimateList = estimateRepository.getListOfEstimateByServiceTiketCode(serviceTicketId);

        //todo: get estimate item list of list estimate ids
        List<Integer> estimateIds = estimateList.stream().map(Estimate::getId).toList();
        List<EstimateItem> estimateItems =estimateItemRepository.findByEstimateIds(estimateIds).stream().filter(estimateItem -> Boolean.FALSE.equals(estimateItem.getIsRemoved())).toList();

        //todo: get work-catalog of estimateItem
        List<Integer> workCategoryId = estimateItems.stream().map(EstimateItem::getWorkCategoryId).filter(Objects::nonNull).distinct().toList();
        Map<Integer, WorkCategory> categoryMap = workCategoryRepo
                .findAllById(workCategoryId)
                .stream()
                .collect(Collectors.toMap(WorkCategory::getId, wc -> wc));

        //todo: group by estimateItem by estimateId
        Map<Integer, List<EstimateItem>> itemsByEstimateId = estimateItems.stream()
                .collect(Collectors.groupingBy(EstimateItem::getEstimateId));
        //todo: map estimateResponĐto
        return estimateList.stream().map(estimate -> {
            EstimateRespondDto dto = estimateDtoMapper.toEstimateDto(estimate);
            List<EstimateItem> items = itemsByEstimateId
                    .getOrDefault(estimate.getId(), List.of());

            BigDecimal totalTax = BigDecimal.ZERO;
            BigDecimal subTotal = BigDecimal.ZERO;
            List<EstimateItemDto> itemDtos = new ArrayList<>();
            for (EstimateItem item : items) {
                EstimateItemDto itemDto = estimateDtoMapper.toEstimateItemDto(item);
                // inject work category to ech items
                WorkCategory wc = categoryMap.get(item.getWorkCategoryId());
                if (wc != null) {
                    itemDto.setWorkCategory(estimateDtoMapper.toWorkCateDto(wc));
                }

                if (Boolean.TRUE.equals(item.getIsChecked())) {

                    // 1. Cộng dồn tiền thuế (chỉ cho item được chọn)
                    if (item.getTaxAmount() != null) {
                        totalTax = totalTax.add(item.getTaxAmount());
                    }

                    // 2. Cộng dồn tiền hàng (chỉ cho item được chọn)
                    BigDecimal itemQty = BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 0);
                    BigDecimal itemPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    subTotal = subTotal.add(itemPrice.multiply(itemQty));
                }
                itemDtos.add(itemDto);
            }
            dto.setTotalPrice(subTotal.add(totalTax));
            dto.setSubTotal(subTotal);
            dto.setTotalTaxAmount(totalTax);
            dto.setItems(itemDtos);
            return dto;
        }).toList();
    }
    @Transactional
    public EstimateRespondDto createEstimate(EstimateRequestDto request) {
        Estimate estimate = new Estimate();
        estimate.setServiceTicketId(request.getServiceTicketId());
        estimate.setEstimateType(request.getEstimateType());
        estimate.setStatus(EstimateEnum.DRAFT);
        estimate.setVersion(1);
        estimate.setTotalPrice(BigDecimal.ZERO);
        Estimate saved = estimateRepository.save(estimate);

        List<EstimateItem> items = resolveItems(request.getItems(), saved.getId());
        estimateItemRepository.saveAll(items);

        //todo: update total_price
        BigDecimal totalPrice = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsChecked()))
                .filter(item -> Boolean.FALSE.equals(item.getIsRemoved()))
                .map(item -> item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO)
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
                WorkCategory wc = workCategoryRepo.findById(req.getWorkCategoryId());
                TaxRule taxRule = (wc != null) ? taxRuleRepository.findById(wc.getTaxRuleId()) : null;
                applyTax(existing,taxRule.getTaxRuleId());
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
                .map(item -> item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO)
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
            WorkCategory workCategory = new WorkCategory();
            // Tạo work_category mới nếu không có sẵn
            if (categoryId == null && req.getNewCategoryName() != null) {
            System.out.println("DEBUG CREATING CATA: ");
                WorkCategory newCategory = new WorkCategory();
                newCategory.setCategoryName(req.getNewCategoryName());
                newCategory.setCategoryCode(
                        req.getNewCategoryName().toUpperCase().replace(" ", "_")
                );
                newCategory.setIsDefault(false);
                newCategory.setIsActive(true);
                newCategory.setTaxRuleId(req.getTaxRuleId() != null ? req.getTaxRuleId() : 1);
                int nextOrder = workCategoryRepo.findMaxDisplayOrder()+1;
                newCategory.setDisplayOrder(nextOrder);
                WorkCategory saved = workCategoryRepo.save(newCategory);
                categoryId = saved.getId();
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
            item.setIsChecked(req.getIsChecked() != null ? req.getIsChecked() : false);

            TaxRule taxRule = null;
            if (workCategory != null && workCategory.getTaxRuleId() != null) {
                taxRule = taxRuleRepository.findById(workCategory.getTaxRuleId());
            }
            //todo: vat calculate
            Integer ruleId = null;
            if (workCategory != null) {
                ruleId = workCategory.getTaxRuleId();
            } else {
                ruleId = req.getTaxRuleId();
            }
            BigDecimal quantity = BigDecimal.valueOf(req.getQuantity());
            BigDecimal unitPrice = req.getUnitPrice();

            BigDecimal totalPrice = unitPrice.multiply(quantity);
            item.setTotalPrice(totalPrice);
            applyTax(item,ruleId);
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
            newCategory.setTaxRuleId(request.getTaxRuleId() != null ? request.getTaxRuleId() : 1);
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
                .map(i -> i.getTotalPrice() != null ? i.getTotalPrice() : BigDecimal.ZERO)
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
}
