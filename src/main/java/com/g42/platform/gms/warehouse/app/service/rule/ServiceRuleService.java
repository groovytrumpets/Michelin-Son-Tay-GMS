package com.g42.platform.gms.warehouse.app.service.rule;

import com.g42.platform.gms.warehouse.api.dto.request.CreateServiceRuleRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateServiceRuleRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ServiceSuggestion;
import com.g42.platform.gms.warehouse.domain.entity.ServiceRule;
import com.g42.platform.gms.warehouse.domain.repository.ServiceRuleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceRuleService {

    private final ServiceRuleRepo serviceRuleRepo;
    @Transactional(readOnly = true)
    public List<ServiceSuggestion> suggest(String vehicleModel, int odometerKm) {
        if (vehicleModel == null || vehicleModel.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehicleModel không được để trống");
        }
        return serviceRuleRepo.findAllActive().stream()
                .filter(rule -> rule.getKmThreshold() <= odometerKm)
                .filter(rule -> vehicleModel.toLowerCase()
                        .contains(rule.getVehicleTypePattern().toLowerCase()))
                .map(this::toSuggestion)
                .collect(Collectors.toList());
    }
    @Transactional
    public ServiceSuggestion create(CreateServiceRuleRequest request, Integer staffId) {
        ServiceRule rule = new ServiceRule();
        rule.setVehicleTypePattern(request.getVehicleTypePattern());
        rule.setKmThreshold(request.getKmThreshold());
        rule.setSuggestedItemIds(request.getSuggestedItemIds());
        rule.setReason(request.getReason());
        rule.setIsActive(true);
        rule.setCreatedBy(staffId);
        return toSuggestion(serviceRuleRepo.save(rule));
    }
    @Transactional
    public ServiceSuggestion update(Integer ruleId, UpdateServiceRuleRequest request) {
        ServiceRule rule = findOrThrow(ruleId);
        if (request.getVehicleTypePattern() != null) rule.setVehicleTypePattern(request.getVehicleTypePattern());
        if (request.getKmThreshold() != null) rule.setKmThreshold(request.getKmThreshold());
        if (request.getSuggestedItemIds() != null) rule.setSuggestedItemIds(request.getSuggestedItemIds());
        if (request.getReason() != null) rule.setReason(request.getReason());
        if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());
        return toSuggestion(serviceRuleRepo.save(rule));
    }
    @Transactional
    public void delete(Integer ruleId) {
        ServiceRule rule = findOrThrow(ruleId);
        rule.setIsActive(false);
        serviceRuleRepo.save(rule);
    }

    private ServiceRule findOrThrow(Integer ruleId) {
        return serviceRuleRepo.findById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy service rule id=" + ruleId));
    }

    private ServiceSuggestion toSuggestion(ServiceRule rule) {
        ServiceSuggestion s = new ServiceSuggestion();
        s.setRuleId(rule.getRuleId());
        s.setVehicleTypePattern(rule.getVehicleTypePattern());
        s.setKmThreshold(rule.getKmThreshold());
        s.setSuggestedItemIds(rule.getSuggestedItemIds());
        s.setReason(rule.getReason());
        return s;
    }
}
