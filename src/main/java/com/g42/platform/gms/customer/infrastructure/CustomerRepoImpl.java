package com.g42.platform.gms.customer.infrastructure;

import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.booking_management.infrastructure.specification.BookingRequestSpecification;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.domain.entity.CustomerAuth;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.customer.domain.repository.CustomerRepo;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerAuthJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import com.g42.platform.gms.customer.infrastructure.mapper.CustomerJpaMapper;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerAuthJpaRepo;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerProfileJpaRepo;
import com.g42.platform.gms.customer.infrastructure.spectification.CustomerProfileSpecification;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@AllArgsConstructor
public class CustomerRepoImpl implements CustomerRepo {
    @Autowired
    CustomerAuthJpaRepo customerAuthJpaRepo;
    @Autowired
    CustomerProfileJpaRepo customerProfileJpaRepo;
    @Autowired
    CustomerJpaMapper customerJpaMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public CustomerProfile createNewCustomerProfile(CustomerCreateDto customerDto) {
    //todo: create new acc based on customerDto
        CustomerProfileJpa entity = new CustomerProfileJpa();
        entity.setFullName(customerDto.getFullName());
        entity.setPhone(customerDto.getPhone());
        entity.setEmail(customerDto.getEmail());
        entity.setGender(customerDto.getGender());
        entity.setAvatar(customerDto.getAvatar());

        // Convert String -> LocalDate
        if (customerDto.getDob() != null && !customerDto.getDob().isBlank()) {
            entity.setDob(LocalDate.parse(customerDto.getDob()));
            // format phải là yyyy-MM-dd
        }
        return customerJpaMapper.toDomain(customerProfileJpaRepo.save(entity));
    }

    @Override
    public CustomerAuth createNewCustomerAuth(CustomerCreateDto customerCreateDto, CustomerProfile customerProfile) {
        CustomerAuthJpa customerAuthJpa = new CustomerAuthJpa();
        customerAuthJpa.setStatus(CustomerStatus.ACTIVE);
        customerAuthJpa.setPinHash(passwordEncoder.encode(customerCreateDto.getPin()));
        customerAuthJpa.setCustomerId(customerProfile.getCustomerId());
        return null;
    }

    @Override
    public Page<CustomerProfile> getListOfCustomers(int page,int size, LocalDate date, Boolean isGuest, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<CustomerProfileJpa> specification = Specification.unrestricted();
        specification = specification.and(CustomerProfileSpecification.filter(date,isGuest));
        if (search != null && !search.isBlank()) {
            specification = specification.and(CustomerProfileSpecification.searchProfiles(search));
        }
        Page<CustomerProfileJpa> customerProfileJpas = customerProfileJpaRepo.findAll(specification, pageable);
        return customerProfileJpas.map(customerJpaMapper::toDomain);
    }
}
