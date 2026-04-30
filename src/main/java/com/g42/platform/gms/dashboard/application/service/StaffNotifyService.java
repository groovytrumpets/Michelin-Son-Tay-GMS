package com.g42.platform.gms.dashboard.application.service;

import com.g42.platform.gms.dashboard.domain.repository.StaffNotifyRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StaffNotifyService {
    @Autowired
    private StaffNotifyRepo staffNotifyRepo;
}
