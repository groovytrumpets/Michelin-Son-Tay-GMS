package com.g42.platform.gms.marketing.service_catalog.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceSumaryRespond;
import com.g42.platform.gms.marketing.service_catalog.application.service.ServiceCatalogService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/home")
public class ServiceController {
    @Autowired
    private final ServiceCatalogService serviceCatalogService;
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<ServiceSumaryRespond>>> getLandingServices() {
        List<ServiceSumaryRespond> respondList = serviceCatalogService.getListActiveServices();
        return ResponseEntity.ok(ApiResponses.success(respondList));
    }
}
