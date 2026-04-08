package com.g42.platform.gms.marketing.service_catalog.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceCreateRequest;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceDetailRespond;
import com.g42.platform.gms.marketing.service_catalog.application.service.ServiceCatalogService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
@RestController
@AllArgsConstructor
@RequestMapping("/api/service")
public class ServiceAdminController {
    @Autowired
    private final ServiceCatalogService serviceCatalogService;
    @PostMapping("create/{catalogId}")
    public ResponseEntity<ApiResponse<ServiceDetailRespond>> createService(@ModelAttribute ServiceCreateRequest request,@PathVariable Integer catalogId) throws IOException {
        return ResponseEntity.ok(ApiResponses.success(serviceCatalogService.createNewService(request,catalogId)));
    }
    @PutMapping("update/{serviceId}")
    public ResponseEntity<ApiResponse<ServiceDetailRespond>> updateService(@ModelAttribute ServiceCreateRequest request,@PathVariable Long serviceId) throws IOException {
        return ResponseEntity.ok(ApiResponses.success(serviceCatalogService.updateService(request,serviceId)));
    }
}
