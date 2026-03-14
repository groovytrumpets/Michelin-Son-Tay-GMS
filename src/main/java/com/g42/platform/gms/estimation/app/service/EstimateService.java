package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.estimation.api.dto.EstimateItemDto;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.estimation.api.dto.request.EstimateItemReqDto;
import com.g42.platform.gms.estimation.api.dto.request.EstimateRequestDto;
import com.g42.platform.gms.estimation.api.mapper.EstimateDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
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

    public List<EstimateRespondDto> getEstimateByCode(Integer serviceTicketId) {
        //todo: find all estimate
        List<Estimate> estimateList = estimateRepository.getListOfEstimateByServiceTiketCode(serviceTicketId);
        //todo: get estimate item list of list estimate ids
        List<Integer> estimateIds = estimateList.stream().map(Estimate::getId).toList();
        List<EstimateItem> estimateItems =estimateItemRepository.findByEstimateIds(estimateIds);
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

            List<EstimateItemDto> itemDtos = items.stream().map(item -> {
                EstimateItemDto itemDto = estimateDtoMapper.toEstimateItemDto(item);
                // inject work category to ech items
                WorkCategory wc = categoryMap.get(item.getWorkCategoryId());
                itemDto.setWorkCategory(estimateDtoMapper.toWorkCateDto(wc));
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
        estimate.setTotalPrice(estimate.getTotalPrices());
        Estimate saved = estimateRepository.save(estimate);

        List<EstimateItem> items = resolveItems(request.getItems(), saved.getId());
        estimateItemRepository.saveAll(items);

        //todo: update total_price
        BigDecimal totalPrice = items.stream()
                .map(item -> item.getSubTotal() != null ? item.getSubTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        saved.setTotalPrice(totalPrice);
        System.out.println("Total_price: "+saved.getTotalPrices());
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
                toSave.add(existing);
                incomingIds.add(req.getItemId());
            } else {
                toSave.addAll(resolveItems(List.of(req), estimateId));
            }
        }
        estimateItems.stream()
                .filter(i -> !incomingIds.contains(i.getId()))
                .forEach(estimateItemRepository::delete);

        estimateItemRepository.saveAll(toSave);
        BigDecimal totalPrice = toSave.stream()
                .map(item -> item.getUnitPrice() == null || item.getQuantity() == null
                        ? BigDecimal.ZERO
                        : item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
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
            return item;
        }).toList();
    }
    private EstimateRespondDto getEstimateRespondDto(Integer estimateId) {
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        if (estimate == null) {
            throw new RuntimeException("Estimate not found");
        }
        List<EstimateItem> items = estimateItemRepository.findByEstimateId(estimateId);

        List<Integer> categoryIds = items.stream()
                .map(EstimateItem::getWorkCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, WorkCategory> categoryMap = workCategoryRepo
                .findAllById(categoryIds).stream()
                .collect(Collectors.toMap(WorkCategory::getId, wc -> wc));

        EstimateRespondDto dto = estimateDtoMapper.toEstimateDto(estimate);
        dto.setItems(items.stream().map(item -> {
            EstimateItemDto itemDto = estimateDtoMapper.toEstimateItemDto(item);
            itemDto.setWorkCategory(
                    estimateDtoMapper.toWorkCateDto(categoryMap.get(item.getWorkCategoryId()))
            );
            return itemDto;
        }).toList());
        return dto;
    }
}
