package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.customer.infrastructure.entity.PointConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointConfigJpaRepo extends JpaRepository<PointConfigJpa, Integer> {
}
