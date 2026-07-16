package com.g42.platform.gms.dashboard.application.service;

import com.g42.platform.gms.dashboard.api.dto.StaffDashboardConfigRequest;
import com.g42.platform.gms.dashboard.infrastructure.entity.StaffDashboardConfigJpa;
import com.g42.platform.gms.dashboard.infrastructure.repository.StaffDashboardConfigJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffDashboardConfigService {

    private final StaffDashboardConfigJpaRepo configRepo;

    public List<StaffDashboardConfigJpa> getAllConfigs(Integer staffId) {
        return configRepo.findAllByStaffId(staffId);
    }

    public Optional<StaffDashboardConfigJpa> getActiveConfig(Integer staffId) {
        return configRepo.findByStaffIdAndIsActiveTrue(staffId);
    }

    @Transactional
    public StaffDashboardConfigJpa createConfig(Integer staffId, StaffDashboardConfigRequest req) {
        StaffDashboardConfigJpa config = new StaffDashboardConfigJpa();
        config.setStaffId(staffId);
        config.setDashboardName(req.getDashboardName());
        config.setLayoutConfig(req.getLayoutConfig());
        config.setIsActive(req.getIsActive() != null ? req.getIsActive() : false);
        
        StaffDashboardConfigJpa saved = configRepo.save(config);
        if (Boolean.TRUE.equals(saved.getIsActive())) {
            deactivateOthers(staffId, saved.getId());
        }
        return saved;
    }

    @Transactional
    public StaffDashboardConfigJpa updateConfig(Integer staffId, Integer configId, StaffDashboardConfigRequest req) {
        StaffDashboardConfigJpa config = configRepo.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Config not found"));
        
        if (!config.getStaffId().equals(staffId)) {
            throw new IllegalStateException("Unauthorized access");
        }

        if (req.getDashboardName() != null) {
            config.setDashboardName(req.getDashboardName());
        }
        if (req.getLayoutConfig() != null) {
            config.setLayoutConfig(req.getLayoutConfig());
        }
        if (req.getIsActive() != null) {
            config.setIsActive(req.getIsActive());
        }

        StaffDashboardConfigJpa saved = configRepo.save(config);
        if (Boolean.TRUE.equals(saved.getIsActive())) {
            deactivateOthers(staffId, saved.getId());
        }
        return saved;
    }

    @Transactional
    public void deleteConfig(Integer staffId, Integer configId) {
        StaffDashboardConfigJpa config = configRepo.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Config not found"));
        
        if (!config.getStaffId().equals(staffId)) {
            throw new IllegalStateException("Unauthorized access");
        }

        configRepo.delete(config);
        
        // If deleted config was active, activate another one if available
        if (Boolean.TRUE.equals(config.getIsActive())) {
            List<StaffDashboardConfigJpa> remaining = configRepo.findAllByStaffId(staffId);
            if (!remaining.isEmpty()) {
                StaffDashboardConfigJpa first = remaining.get(0);
                first.setIsActive(true);
                configRepo.save(first);
            }
        }
    }

    @Transactional
    public StaffDashboardConfigJpa activateConfig(Integer staffId, Integer configId) {
        StaffDashboardConfigJpa config = configRepo.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Config not found"));
        
        if (!config.getStaffId().equals(staffId)) {
            throw new IllegalStateException("Unauthorized access");
        }

        config.setIsActive(true);
        StaffDashboardConfigJpa saved = configRepo.save(config);
        deactivateOthers(staffId, saved.getId());
        return saved;
    }

    private void deactivateOthers(Integer staffId, Integer activeId) {
        List<StaffDashboardConfigJpa> list = configRepo.findAllByStaffId(staffId);
        for (StaffDashboardConfigJpa config : list) {
            if (!config.getId().equals(activeId) && Boolean.TRUE.equals(config.getIsActive())) {
                config.setIsActive(false);
                configRepo.save(config);
            }
        }
    }
}
