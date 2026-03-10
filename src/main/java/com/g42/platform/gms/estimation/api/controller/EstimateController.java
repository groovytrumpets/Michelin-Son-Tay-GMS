package com.g42.platform.gms.estimation.api.controller;


import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.estimation.app.service.EstimateService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/service-ticket/estimate/")
public class EstimateController {
    private final EstimateService estimateService;
    @GetMapping("/{serviceTicketId}")
    public ResponseEntity<ApiResponse<List<EstimateRespondDto>>> getEstimateTicketByCode(@PathVariable Integer serviceTicketId){
        List<EstimateRespondDto> estimate = estimateService.getEstimateByCode(serviceTicketId);
        return ResponseEntity.ok(ApiResponses.success(estimate));
    }

}
