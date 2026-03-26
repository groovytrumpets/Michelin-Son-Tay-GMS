package com.g42.platform.gms.warehouse.api.controller;

import com.g42.platform.gms.warehouse.app.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {
    @Autowired
    private WarehouseService warehouseService;
}
