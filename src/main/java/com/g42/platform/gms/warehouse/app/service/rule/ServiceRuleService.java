package com.g42.platform.gms.warehouse.app.service.rule;

import com.g42.platform.gms.warehouse.api.dto.request.CreateServiceRuleRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateServiceRuleRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ServiceSuggestion;

import java.util.List;

public interface ServiceRuleService {

    /** Gợi ý dịch vụ theo xe và km */
    List<ServiceSuggestion> suggest(String vehicleModel, int odometerKm);

    /** CRUD cho MANAGER */
    ServiceSuggestion create(CreateServiceRuleRequest request, Integer staffId);
    ServiceSuggestion update(Integer ruleId, UpdateServiceRuleRequest request);
    void delete(Integer ruleId);
}
