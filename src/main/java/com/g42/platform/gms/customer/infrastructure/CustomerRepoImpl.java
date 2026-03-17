package com.g42.platform.gms.customer.infrastructure;

import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.auth.mapper.CustomerProfileMapper;
import com.g42.platform.gms.booking_management.infrastructure.specification.BookingRequestSpecification;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.domain.entity.CustomerAuth;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.customer.domain.exception.CustomerErrorCode;
import com.g42.platform.gms.customer.domain.exception.CustomerException;
import com.g42.platform.gms.customer.domain.repository.CustomerRepo;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerAuthJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import com.g42.platform.gms.customer.infrastructure.mapper.CustomerAuthJpaMapper;
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
import java.time.LocalDateTime;

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
    CustomerAuthJpaMapper customerAuthJpaMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CustomerProfileMapper customerProfileMapper;

    @Override
    public CustomerProfile createNewCustomerProfile(CustomerCreateDto customerDto) {
        if (customerDto.getPhone()==null){
            throw new CustomerException("Phone must not null!", CustomerErrorCode.INVALID_PHONE);
        }
    //todo: create new acc based on customerDto
        CustomerProfileJpa entity = new CustomerProfileJpa();
        entity.setFullName(customerDto.getFullName());
        entity.setPhone(customerDto.getPhone());
        entity.setEmail(customerDto.getEmail());
        entity.setGender(customerDto.getGender());
        entity.setAvatar(customerDto.getAvatar());


        if (customerDto.getDob() != null && !customerDto.getDob().isBlank()) {
            entity.setDob(LocalDate.parse(customerDto.getDob()));
        }
        return customerJpaMapper.toDomain(customerProfileJpaRepo.save(entity));
    }

    @Override
    public CustomerAuth createNewCustomerAuth(CustomerCreateDto customerCreateDto, CustomerProfile customerProfile) {
        if (customerProfile.getCustomerId()==null){throw new CustomerException("Customer Id must not null!", CustomerErrorCode.INVALID_CUSTOMER_PROFILE);}
        CustomerAuthJpa customerAuthJpa = new CustomerAuthJpa();
        customerAuthJpa.setStatus(CustomerStatus.ACTIVE);
        customerAuthJpa.setPinHash(passwordEncoder.encode(customerCreateDto.getPin()));
        customerAuthJpa.setCustomerId(customerProfile.getCustomerId());
        customerAuthJpa.setCreatedAt(LocalDateTime.now());
        return customerAuthJpaMapper.toJpa(customerAuthJpaRepo.save(customerAuthJpa));
    }

    @Override
    public Page<CustomerProfile> getListOfCustomers(int page,int size, LocalDate date, Boolean isGuest, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<CustomerProfileJpa> specification = Specification.unrestricted();
        specification = specification.and(CustomerProfileSpecification.filter(date));
        if (search != null && !search.isBlank()) {
            specification = specification.and(CustomerProfileSpecification.searchProfiles(search));
        }
        Page<CustomerProfileJpa> customerProfileJpas = customerProfileJpaRepo.findAll(specification, pageable);
        return customerProfileJpas.map(jpa -> {
            CustomerProfile profile = customerJpaMapper.toDomain(jpa);
            CustomerAuthJpa auth = customerAuthJpaRepo.findByCustomerId(jpa.getCustomerId());
            if (auth != null) profile.setStatus(auth.getStatus());
            return profile;
        });
    }

    @Override
    public CustomerProfile findProflieById(Integer customerId) {
        CustomerProfileJpa customerProfileJpa = customerProfileJpaRepo.findByCustomerId(customerId);
        CustomerAuthJpa customerAuthJpa = customerAuthJpaRepo.findByCustomerId(customerId);
        if (customerProfileJpa==null||customerAuthJpa==null) throw new CustomerException("Customer not found!", CustomerErrorCode.INVALID_CUSTOMER_PROFILE);

        return customerJpaMapper.toDomain(customerProfileJpa);
    }

    @Override
    public CustomerAuth findAuthById(Integer customerId) {
        CustomerAuthJpa customerAuthJpa = customerAuthJpaRepo.findByCustomerId(customerId);
        return customerAuthJpaMapper.toJpa(customerAuthJpa);
    }

    @Override
    public boolean updateCustomer(Integer customerId, CustomerProfile customerProfile, CustomerAuth customerAuth) {
        CustomerProfileJpa customerProfileJpa = customerProfileJpaRepo.save(customerJpaMapper.toJpa(customerProfile));
        CustomerAuthJpa customerAuthJpa = customerAuthJpaRepo.save(customerAuthJpaMapper.toDomain(customerAuth));
        return (customerAuthJpa!=null && customerProfileJpa!=null);

    }

    @Override
    public CustomerProfile findCustomerById(Integer customerId) {
        return customerJpaMapper.toDomain(customerProfileJpaRepo.findByCustomerId(customerId));
    }
}
