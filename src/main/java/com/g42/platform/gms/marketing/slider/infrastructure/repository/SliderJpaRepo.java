package com.g42.platform.gms.marketing.slider.infrastructure.repository;

import com.g42.platform.gms.marketing.slider.infrastructure.entity.SliderJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SliderJpaRepo extends JpaRepository<SliderJpa, Integer> {
    Optional<SliderJpa> findByLocationCode(String locationCode);
}
