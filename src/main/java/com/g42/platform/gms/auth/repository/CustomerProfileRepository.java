package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProfileRepository
        extends JpaRepository<CustomerProfile, Integer> {

    Optional<CustomerProfile> findByPhone(String phone);

    CustomerProfile getCustomerProfilesByCustomerId(Integer customerId);
}

