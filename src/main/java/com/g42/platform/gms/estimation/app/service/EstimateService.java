package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.estimation.api.dto.EstimateItemDto;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.estimation.api.mapper.EstimateDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.estimation.domain.repository.WorkCategoryRepository;
import com.g42.platform.gms.estimation.infrastructure.mapper.EstimateItemJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateRepositoryJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstimateService {
    private final EstimateRepository estimateRepositoryJpa;
    private final EstimateItemRepository estimateItemRepository;
    private final WorkCategoryRepository workCategory;
    private final EstimateDtoMapper estimateDtoMapper;

    public List<EstimateRespondDto> getEstimateByCode(Integer serviceTicketId) {
        //todo: find all estimate
        List<Estimate> estimateList = estimateRepositoryJpa.getListOfEstimateByServiceTiketCode(serviceTicketId);
        //todo: get estimate item list of list estimate ids
        List<Integer> estimateIds = estimateList.stream().map(Estimate::getId).toList();
        List<EstimateItem> estimateItems =estimateItemRepository.findByEstimateIds(estimateIds);
        //todo: get work-catalog of estimateItem
        List<Integer> workCategoryId = estimateItems.stream().map(EstimateItem::getWorkCategoryId).filter(Objects::nonNull).distinct().toList();
        Map<Integer, WorkCategory> categoryMap = workCategory
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
                itemDto.setWorkCategory(estimateDtoMapper.toDto(wc));
                return itemDto;
            }).toList();

            dto.setItems(itemDtos);
            return dto;
        }).toList();
    }
}
