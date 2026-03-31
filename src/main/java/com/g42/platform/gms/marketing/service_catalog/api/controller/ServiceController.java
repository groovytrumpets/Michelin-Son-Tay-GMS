package com.g42.platform.gms.marketing.service_catalog.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceCreateRequest;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceDetailRespond;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceSumaryRespond;
import com.g42.platform.gms.marketing.service_catalog.application.service.ServiceCatalogService;
import com.g42.platform.gms.warehouse.api.dto.CatalogCreateDto;
import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/home")
public class ServiceController {
    @Autowired
    private final ServiceCatalogService serviceCatalogService;
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<ServiceSumaryRespond>>> getLandingServices() {
        List<ServiceSumaryRespond> respondList = serviceCatalogService.getListActiveServices();
        return ResponseEntity.ok(ApiResponses.success(respondList));
    }
    @GetMapping("/service/{serviceId}")
    public ResponseEntity<ApiResponse<ServiceDetailRespond>> getServiceDetail(@PathVariable Long serviceId) {
        ServiceDetailRespond serviceDetailRespond = serviceCatalogService.getServiceDetailById(serviceId);
        return ResponseEntity.ok(ApiResponses.success(serviceDetailRespond));
    }
    @GetMapping("/catalog")
    public Long [] getCatalogId(@RequestParam Long[] serviceId) {
        return serviceCatalogService.getArrayOfCatalogId(serviceId);
    }

}
