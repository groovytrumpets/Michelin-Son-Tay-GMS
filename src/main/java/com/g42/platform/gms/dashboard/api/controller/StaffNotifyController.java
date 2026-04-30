package com.g42.platform.gms.dashboard.api.controller;

import com.g42.platform.gms.dashboard.application.service.StaffNotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notify")
public class StaffNotifyController {
    @Autowired
    private StaffNotifyService staffNotifyService;
}
