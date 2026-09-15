package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.customer.infrastructure.entity.CustomerPhoneJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerPhoneJpaRepo extends JpaRepository<CustomerPhoneJpa, Integer> {

    Optional<CustomerPhoneJpa> findByPhone(String phone);

    List<CustomerPhoneJpa> findByCustomerIdOrderByCustomerPhoneIdAsc(Integer customerId);

    List<CustomerPhoneJpa> findByCustomerIdIn(Collection<Integer> customerIds);

    void deleteByCustomerId(Integer customerId);
}
