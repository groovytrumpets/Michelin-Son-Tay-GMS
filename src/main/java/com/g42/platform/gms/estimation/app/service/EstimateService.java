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
        //todo:add tax rule
        List<Integer> taxRuleIds = estimateItems.stream()
                .map(EstimateItem::getTaxRuleId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Integer, TaxRule> taxRuleMap = taxRuleRepository.findAllByIds(taxRuleIds)
                .stream()
                .collect(Collectors.toMap(TaxRule::getTaxRuleId, tr -> tr));
        //todo: group by estimateItem by estimateId
        Map<Integer, List<EstimateItem>> itemsByEstimateId = estimateItems.stream()
                .collect(Collectors.groupingBy(EstimateItem::getEstimateId));
        //todo: map estimateResponĐto
        return estimateList.stream().map(estimate -> {
            EstimateRespondDto dto = estimateDtoMapper.toEstimateDto(estimate);
            List<EstimateItem> items = itemsByEstimateId
                    .getOrDefault(estimate.getId(), List.of());

            List<EstimateItemDto> itemDtos = items.stream().map(item -> {
                EstimateItemDto itemDto = estimateDtoMapper.toEstimateItemDto(item);
                // inject work category to ech items
                WorkCategory wc = categoryMap.get(item.getWorkCategoryId());
                itemDto.setWorkCategory(estimateDtoMapper.toWorkCateDto(wc));
                //injection for tax rule
                TaxRule taxRule = taxRuleMap.get(item.getTaxRuleId());
                if (taxRule != null) {
                    itemDto.setTaxCode(taxRule.getTaxCode());
                    itemDto.setTaxRate(taxRule.getTaxRate());

                if (item.getUnitPrice() != null && taxRule.getTaxRate() != null) {
                    BigDecimal vatPerUnit = item.getUnitPrice().multiply(
                            taxRule.getTaxRate().divide(BigDecimal.valueOf(100)));
                    itemDto.setUnitPriceWithVat(item.getUnitPrice().add(vatPerUnit));
                    BigDecimal unitPriceWithVat = item.getUnitPrice().add(vatPerUnit);
                    itemDto.setSubTotalWithVat(unitPriceWithVat.multiply(
                            BigDecimal.valueOf(item.getQuantity())));
                }
                }else {
                    itemDto.setUnitPriceWithVat(item.getUnitPrice());
                }
                return itemDto;
            }).toList();

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
        estimate.setTotalPrice(estimate.getTotalPrice());
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
                applyTax(existing);
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

            // Tạo work_category mới nếu không có sẵn
            if (categoryId == null && req.getNewCategoryName() != null) {
                WorkCategory newCategory = new WorkCategory();
                newCategory.setCategoryName(req.getNewCategoryName());
                newCategory.setCategoryCode(
                        req.getNewCategoryName().toUpperCase().replace(" ", "_")
                );
                newCategory.setIsDefault(false);
                newCategory.setIsActive(true);
                int nextOrder = workCategoryRepo.findMaxDisplayOrder()+1;
                newCategory.setDisplayOrder(nextOrder);
                WorkCategory saved = workCategoryRepo.save(newCategory);
                categoryId = saved.getId();
            }

            EstimateItem item = new EstimateItem();
            item.setEstimateId(estimateId);
            item.setWorkCategoryId(categoryId);
            item.setItemId(req.getItemId());
            item.setItemName(req.getItemName());
            item.setQuantity(req.getQuantity());
            item.setUnitPrice(req.getUnitPrice());
            item.setTaxRuleId(req.getTaxRuleId());
            item.setIsChecked(req.getIsChecked() != null ? req.getIsChecked() : false);
            //todo: vat calculate
            applyTax(item);
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

        List<Integer> taxRuleIds = items.stream()
                .map(EstimateItem::getTaxRuleId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Integer, TaxRule> taxRuleMap = taxRuleRepository.findAllByIds(taxRuleIds).stream()
                .collect(Collectors.toMap(TaxRule::getTaxRuleId, tr -> tr));
        System.out.println("taxRuleIds: " + taxRuleIds);
        System.out.println("taxRuleMap size: " + taxRuleMap.size());
        EstimateRespondDto dto = estimateDtoMapper.toEstimateDto(estimate);
        dto.setItems(items.stream().map(item -> {
            EstimateItemDto itemDto = estimateDtoMapper.toEstimateItemDto(item);
            itemDto.setWorkCategory(
                    estimateDtoMapper.toWorkCateDto(categoryMap.get(item.getWorkCategoryId()))
            );
                    //todo: add inject tax info
            TaxRule taxRule = taxRuleMap.get(item.getTaxRuleId());
            if (taxRule != null) {
                itemDto.setTaxRuleId(taxRule.getTaxRuleId());
                itemDto.setTaxCode(taxRule.getTaxCode());
                itemDto.setTaxRate(taxRule.getTaxRate());
                if (item.getUnitPrice() != null && taxRule.getTaxRate() != null) {
                    BigDecimal vatPerUnit = item.getUnitPrice().multiply(
                            taxRule.getTaxRate().divide(BigDecimal.valueOf(100)));
                    itemDto.setUnitPriceWithVat(item.getUnitPrice().add(vatPerUnit));
                    BigDecimal unitPriceWithVat = item.getUnitPrice().add(vatPerUnit);
                    itemDto.setSubTotalWithVat(unitPriceWithVat.multiply(
                            BigDecimal.valueOf(item.getQuantity())));
                }
            }else {
                itemDto.setUnitPriceWithVat(item.getUnitPrice());
            }
            return itemDto;
        }).toList());
        return dto;
    }

    public EstimateItemReqDto updateEstimateItem(Integer estimateItemId, EstimateItemReqDto request) {
        EstimateItem estimateItem = estimateItemRepository.findByEstimateItemId(estimateItemId);


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
            int nextOrder = workCategoryRepo.findMaxDisplayOrder() + 1;
            newCategory.setDisplayOrder(nextOrder);
            WorkCategory saved = workCategoryRepo.save(newCategory);
            estimateItem.setWorkCategoryId(saved.getId());
        }
        //todo: handle newCate
        if (request.getItemId() != null)estimateItem.setItemId(request.getItemId());
        if (request.getItemName() != null)estimateItem.setItemName(request.getItemName());
        if (request.getQuantity() != null)estimateItem.setQuantity(request.getQuantity());
        if (request.getUnitPrice() != null)estimateItem.setUnitPrice(request.getUnitPrice());
        if (request.getTaxRuleId() != null)estimateItem.setTaxRuleId(request.getTaxRuleId());
        if (request.getIsChecked() != null)estimateItem.setIsChecked(request.getIsChecked());
        if (request.getIsRemoved() != null)estimateItem.setIsRemoved(request.getIsRemoved());
        applyTax(estimateItem);
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
    private void applyTax(EstimateItem item) {
        if (item.getTaxRuleId() != null) {
            TaxRule taxRule = taxRuleRepository.findById(item.getTaxRuleId());
            if (taxRule != null && taxRule.getTaxRate() != null) {
                BigDecimal vatPerUnit = item.getUnitPrice().multiply(
                        taxRule.getTaxRate().divide(BigDecimal.valueOf(100)));
                BigDecimal unitPriceWithVat = item.getUnitPrice().add(vatPerUnit);
                item.setTotalPrice(unitPriceWithVat.multiply(BigDecimal.valueOf(item.getQuantity())));
                return;
            }
        }
        item.setTotalPrice(item.getSubTotal());
    }

    public EstimateRespondDto updateEstimateApprove(Integer estimateId) {
        Estimate estimate =  estimateRepository.findEstimateById(estimateId);
        estimate.setStatus(EstimateEnum.APPROVED);
        Estimate saved = estimateRepository.save(estimate);
        return estimateDtoMapper.toEstimateDto(saved);
    }
}
