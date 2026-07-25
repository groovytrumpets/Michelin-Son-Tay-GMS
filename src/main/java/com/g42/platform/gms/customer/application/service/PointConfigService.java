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
            c.setRankSilverPoints(5000);
            c.setRankGoldPoints(15000);
            c.setRankPlatinumPoints(30000);
            c.setRankDiamondPoints(50000);
            c.setDealerLevel2Points(50000);
            c.setDealerLevel3Points(150000);
            c.setDealerLevel4Points(300000);
            c.setDealerLevel5Points(500000);
            return c;
        });

        return new PointConfigDto(
                config.getPointsPer1000Vnd(),
                config.getBonusPointsPerService(),
                config.getPointsPerReferral(),
                config.getRankSilverPoints(),
                config.getRankGoldPoints(),
                config.getRankPlatinumPoints(),
                config.getRankDiamondPoints(),
                config.getDealerLevel2Points(),
                config.getDealerLevel3Points(),
                config.getDealerLevel4Points(),
                config.getDealerLevel5Points()
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
        config.setRankSilverPoints(dto.getRankSilverPoints());
        config.setRankGoldPoints(dto.getRankGoldPoints());
        config.setRankPlatinumPoints(dto.getRankPlatinumPoints());
        config.setRankDiamondPoints(dto.getRankDiamondPoints());
        config.setDealerLevel2Points(dto.getDealerLevel2Points());
        config.setDealerLevel3Points(dto.getDealerLevel3Points());
        config.setDealerLevel4Points(dto.getDealerLevel4Points());
        config.setDealerLevel5Points(dto.getDealerLevel5Points());
        config.setUpdatedAt(LocalDateTime.now());

        pointConfigRepo.save(config);

        return dto;
    }
}
