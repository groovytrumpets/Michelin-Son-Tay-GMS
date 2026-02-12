package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.CustomerAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerAuthRepository
        extends JpaRepository<CustomerAuth, Integer> {

    Optional<CustomerAuth> findByCustomerId(Integer customerId);
}

