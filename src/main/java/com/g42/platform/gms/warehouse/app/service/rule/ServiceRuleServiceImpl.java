package com.g42.platform.gms.warehouse.app.service.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.warehouse.api.dto.request.CreateServiceRuleRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateServiceRuleRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ServiceSuggestion;
import com.g42.platform.gms.warehouse.infrastructure.entity.ServiceRuleJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.ServiceRuleJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRuleServiceImpl implements ServiceRuleService {

    private final ServiceRuleJpaRepo serviceRuleJpaRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceSuggestion> suggest(String vehicleModel, int odometerKm) {
        if (vehicleModel == null || vehicleModel.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehicleModel không được để trống");
        }

        return serviceRuleJpaRepo.findAllByIsActiveTrue().stream()
                .filter(rule -> rule.getKmThreshold() <= odometerKm)
                .filter(rule -> vehicleModel.toLowerCase()
                        .contains(rule.getVehicleTypePattern().toLowerCase()))
                .map(this::toSuggestion)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ServiceSuggestion create(CreateServiceRuleRequest request, Integer staffId) {
        ServiceRuleJpa rule = new ServiceRuleJpa();
        rule.setVehicleTypePattern(request.getVehicleTypePattern());
        rule.setKmThreshold(request.getKmThreshold());
        rule.setSuggestedItemIds(toJson(request.getSuggestedItemIds()));
        rule.setReason(request.getReason());
        rule.setIsActive(true);
        rule.setCreatedBy(staffId);
        return toSuggestion(serviceRuleJpaRepo.save(rule));
    }

    @Override
    @Transactional
    public ServiceSuggestion update(Integer ruleId, UpdateServiceRuleRequest request) {
        ServiceRuleJpa rule = findOrThrow(ruleId);
        if (request.getVehicleTypePattern() != null) rule.setVehicleTypePattern(request.getVehicleTypePattern());
        if (request.getKmThreshold() != null) rule.setKmThreshold(request.getKmThreshold());
        if (request.getSuggestedItemIds() != null) rule.setSuggestedItemIds(toJson(request.getSuggestedItemIds()));
        if (request.getReason() != null) rule.setReason(request.getReason());
        if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());
        return toSuggestion(serviceRuleJpaRepo.save(rule));
    }

    @Override
    @Transactional
    public void delete(Integer ruleId) {
        ServiceRuleJpa rule = findOrThrow(ruleId);
        rule.setIsActive(false);
        serviceRuleJpaRepo.save(rule);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ServiceRuleJpa findOrThrow(Integer ruleId) {
        return serviceRuleJpaRepo.findById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy service rule id=" + ruleId));
    }

    private ServiceSuggestion toSuggestion(ServiceRuleJpa rule) {
        ServiceSuggestion s = new ServiceSuggestion();
        s.setRuleId(rule.getRuleId());
        s.setVehicleTypePattern(rule.getVehicleTypePattern());
        s.setKmThreshold(rule.getKmThreshold());
        s.setSuggestedItemIds(fromJson(rule.getSuggestedItemIds()));
        s.setReason(rule.getReason());
        return s;
    }

    private String toJson(List<Integer> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi serialize JSON");
        }
    }

    private List<Integer> fromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
