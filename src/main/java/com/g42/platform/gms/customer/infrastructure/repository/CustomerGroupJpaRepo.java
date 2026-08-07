package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.customer.infrastructure.entity.CustomerGroupJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerGroupJpaRepo extends JpaRepository<CustomerGroupJpa, Integer> {
    List<CustomerGroupJpa> findAllByOrderByNameAsc();

    List<CustomerGroupJpa> findByActiveTrueOrderByNameAsc();

    Optional<CustomerGroupJpa> findByCodeIgnoreCase(String code);
}
