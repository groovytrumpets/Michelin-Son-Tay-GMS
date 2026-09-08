package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.customer.infrastructure.entity.CustomerAuthJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CustomerAuthJpaRepo extends JpaRepository<CustomerAuthJpa,Integer> {
    CustomerAuthJpa findByCustomerId(Integer customerId);

    /** Nạp bản ghi bảo mật của cả một trang khách trong một truy vấn, tránh N+1. */
    List<CustomerAuthJpa> findByCustomerIdIn(Collection<Integer> customerIds);
}
