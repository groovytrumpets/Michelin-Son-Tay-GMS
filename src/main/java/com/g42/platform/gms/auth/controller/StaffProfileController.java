package com.g42.platform.gms.auth.controller;

import com.g42.platform.gms.auth.dto.StaffAuthDto;
import com.g42.platform.gms.auth.service.StaffAuthService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/staff-profile")
public class StaffProfileController {
    @Autowired
    private final StaffAuthService staffAuthService;
    @GetMapping
    public Iterable<StaffAuthDto> getAllStaffAuth(){
        return staffAuthService.getAllStaffAuth();
    }
}
