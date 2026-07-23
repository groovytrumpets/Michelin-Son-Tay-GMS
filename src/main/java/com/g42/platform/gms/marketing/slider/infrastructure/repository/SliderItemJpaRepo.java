package com.g42.platform.gms.marketing.slider.infrastructure.repository;

import com.g42.platform.gms.marketing.slider.infrastructure.entity.SliderItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SliderItemJpaRepo extends JpaRepository<SliderItemJpa, Integer> {
    List<SliderItemJpa> findBySliderIdOrderByDisplayOrderAsc(Integer sliderId);
}
