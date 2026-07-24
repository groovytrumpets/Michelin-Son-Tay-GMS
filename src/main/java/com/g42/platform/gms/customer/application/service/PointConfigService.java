package com.g42.platform.gms.customer.application.service;

import com.g42.platform.gms.customer.api.dto.PointConfigDto;
import com.g42.platform.gms.customer.infrastructure.entity.PointConfigJpa;
import com.g42.platform.gms.customer.infrastructure.repository.PointConfigJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PointConfigService {

    private final PointConfigJpaRepo pointConfigRepo;

    public PointConfigDto getConfig() {
        PointConfigJpa config = pointConfigRepo.findById(1).orElseGet(() -> {
            PointConfigJpa c = new PointConfigJpa();
            c.setId(1);
            c.setPointsPer1000Vnd(1);
            c.setBonusPointsPerService(10);
            c.setPointsPerReferral(50);
            return c;
        });

        return new PointConfigDto(
                config.getPointsPer1000Vnd(),
                config.getBonusPointsPerService(),
                config.getPointsPerReferral()
        );
    }

    @Transactional
    public PointConfigDto updateConfig(PointConfigDto dto) {
        PointConfigJpa config = pointConfigRepo.findById(1).orElseGet(() -> {
            PointConfigJpa c = new PointConfigJpa();
            c.setId(1);
            return c;
        });

        config.setPointsPer1000Vnd(dto.getPointsPer1000Vnd());
        config.setBonusPointsPerService(dto.getBonusPointsPerService());
        config.setPointsPerReferral(dto.getPointsPerReferral());
        config.setUpdatedAt(LocalDateTime.now());

        pointConfigRepo.save(config);

        return dto;
    }
}
