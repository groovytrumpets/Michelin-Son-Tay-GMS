package com.g42.platform.gms.estimation.api.controller;


import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.estimation.api.dto.request.EstimateRequestDto;
import com.g42.platform.gms.estimation.app.service.EstimateService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping()
    public ResponseEntity<ApiResponse<EstimateRespondDto>> createEstimate(@RequestBody EstimateRequestDto request){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponses.success(estimateService.createEstimate(request))
        );
    }
    @PutMapping("/{estimateId}")
    public ResponseEntity<ApiResponse<EstimateRespondDto>> updateEstimate(@PathVariable Integer estimateId, @RequestBody EstimateRequestDto request){
        return ResponseEntity.ok(
                ApiResponses.success(estimateService.updateEstimate(estimateId, request))
        );
    }

}
