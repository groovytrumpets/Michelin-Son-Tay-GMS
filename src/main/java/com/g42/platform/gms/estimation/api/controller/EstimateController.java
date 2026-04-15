package com.g42.platform.gms.estimation.api.controller;


import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.estimation.api.dto.StockAllocationDto;
import com.g42.platform.gms.estimation.api.dto.WorkCataDto;
import com.g42.platform.gms.estimation.api.dto.request.EstimateItemReqDto;
import com.g42.platform.gms.estimation.api.dto.request.EstimateRequestDto;
import com.g42.platform.gms.estimation.app.service.EstimateService;
import com.g42.platform.gms.estimation.app.service.StockAllocationService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/service-ticket/estimate/")
public class EstimateController {
    private final EstimateService estimateService;
    private final StockAllocationService stockAllocationService;

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
    @PutMapping("/{estimateItemId}/item")
    public ResponseEntity<ApiResponse<EstimateItemReqDto>> updateEstimateItem(@PathVariable Integer estimateItemId, @RequestBody EstimateItemReqDto request){
        return ResponseEntity.ok(
                ApiResponses.success(estimateService.updateEstimateItem(estimateItemId, request))
        );
    }
    @PutMapping("/{estimateId}/{status}")
    public ResponseEntity<ApiResponse<EstimateRespondDto>> updateEstimateApprove(@PathVariable Integer estimateId,@PathVariable EstimateEnum status){
        return ResponseEntity.ok(
                ApiResponses.success(estimateService.updateEstimateStatus(estimateId,status))
        );
    }
    @GetMapping("/work-category/all")
    public ResponseEntity<ApiResponse<List<WorkCataDto>>> getWorkCateList(){
        List<WorkCataDto> workCataDtos = estimateService.getWorkCateList();
        return ResponseEntity.ok(ApiResponses.success(workCataDtos));
    }

    @PostMapping("/{estimateId}/stock-allocation")
    public ResponseEntity<ApiResponse<List<StockAllocationDto>>> createStockAllocation(@PathVariable Integer estimateId,                                                                                       @AuthenticationPrincipal StaffPrincipal principal){
        return ResponseEntity.ok(
                ApiResponses.success(stockAllocationService.createStockAllocation(estimateId,principal.getStaffId())));
    }
    @PutMapping("/{estimateId}/stock-allocation/update")
    public ResponseEntity<ApiResponse<List<StockAllocationDto>>> updateStockAllocation(@PathVariable Integer estimateId,                                                                                       @AuthenticationPrincipal StaffPrincipal principal
    ,@RequestBody List<StockAllocationDto> stockAllocationDtos){
        return ResponseEntity.ok(
                ApiResponses.success(stockAllocationService.updateStockAllocation(estimateId,principal.getStaffId(),stockAllocationDtos)));
    }
    @GetMapping("/{estimateId}/stock-allocation-get")
    public ResponseEntity<ApiResponse<List<StockAllocationDto>>> getStockAllocationByEstimateId(@PathVariable Integer estimateId){
        return ResponseEntity.ok(
                ApiResponses.success(stockAllocationService.getStockAllocationByEstimate(estimateId)));
    }

}
