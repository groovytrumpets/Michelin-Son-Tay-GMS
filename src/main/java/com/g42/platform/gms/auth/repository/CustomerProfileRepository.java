package com.g42.platform.gms.auth.repository;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerProfileRepository
        extends JpaRepository<CustomerProfile, Integer> {

    Optional<CustomerProfile> findByPhone(String phone);

    CustomerProfile getCustomerProfilesByCustomerId(Integer customerId);
    @Query("""
    select c from CustomerProfileJpa c 
    join ServiceTicketManagement st on st.customerId = c.customerId 
    where st.serviceTicketId = :serviceTicketId
""")
    CustomerProfileJpa getCustomerProfilesByServiceTicketId(@Param("serviceTicketId") Integer serviceTicketId);
}

