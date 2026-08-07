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
import com.g42.platform.gms.customer.infrastructure.entity.CustomerPointsJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import com.g42.platform.gms.customer.infrastructure.mapper.CustomerAuthJpaMapper;
import com.g42.platform.gms.customer.infrastructure.mapper.CustomerJpaMapper;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerAuthJpaRepo;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerPointsJpaRepo;
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
    @Autowired
    private CustomerPointsJpaRepo customerPointsJpaRepo;
    @Autowired
    private com.g42.platform.gms.customer.infrastructure.repository.CustomerPointsHistoryJpaRepo customerPointsHistoryJpaRepo;
    @Autowired
    private com.g42.platform.gms.customer.infrastructure.repository.CustomerGroupJpaRepo customerGroupJpaRepo;

    @Override
    public CustomerProfile createNewCustomerProfile(CustomerCreateDto customerDto) {
        if (customerDto.getPhone()==null){
            throw new CustomerException("Phone must not null!", CustomerErrorCode.INVALID_PHONE);
        }
        
        // Kiểm tra xem khách hàng đã tồn tại chưa (khách vãng lai đã có profile nhưng chưa có tài khoản)
        CustomerProfileJpa entity = customerProfileJpaRepo.findByPhone(customerDto.getPhone());
        if (entity == null) {
            entity = new CustomerProfileJpa();
        }
        
        entity.setFullName(customerDto.getFullName());
        entity.setPhone(customerDto.getPhone());
        entity.setEmail(customerDto.getEmail());
        entity.setGender(customerDto.getGender());
        entity.setAvatar(customerDto.getAvatar());
        if (customerDto.getCustomerType() != null) {
            entity.setCustomerType(customerDto.getCustomerType());
        }
        if (customerDto.getIsDealer() != null) {
            entity.setIsDealer(customerDto.getIsDealer());
        }
        
        if (customerDto.getReferrerPhone() != null && !customerDto.getReferrerPhone().isBlank()) {
            CustomerProfileJpa referrer = customerProfileJpaRepo.findByPhone(customerDto.getReferrerPhone());
            if (referrer != null) {
                entity.setReferrerId(referrer.getCustomerId());
            }
        }

        if (customerDto.getDob() != null && !customerDto.getDob().isBlank()) {
            entity.setDob(LocalDate.parse(customerDto.getDob()));
        }

        applyPartnerFields(entity, customerDto);

        CustomerProfileJpa saved = customerProfileJpaRepo.save(entity);
        // Mã khách hàng để trống thì sinh tự động theo id (KH00001, KH00002, ...)
        if (saved.getCustomerCode() == null || saved.getCustomerCode().isBlank()) {
            saved.setCustomerCode(String.format("KH%05d", saved.getCustomerId()));
            saved = customerProfileJpaRepo.save(saved);
        }
        return customerJpaMapper.toDomain(saved);
    }

    /** Gán các trường mở rộng của Danh bạ đối tác từ DTO tạo mới. */
    private void applyPartnerFields(CustomerProfileJpa entity, CustomerCreateDto dto) {
        String code = dto.getCustomerCode() == null ? null : dto.getCustomerCode().trim();
        if (code != null && !code.isEmpty()) {
            ensureCustomerCodeAvailable(code, entity.getCustomerId());
            entity.setCustomerCode(code);
        }

        entity.setTaxCode(dto.getTaxCode());
        entity.setProvinceId(dto.getProvinceId());
        entity.setProvinceName(dto.getProvinceName());
        entity.setDistrictId(dto.getDistrictId());
        entity.setDistrictName(dto.getDistrictName());
        entity.setWardId(dto.getWardId());
        entity.setWardName(dto.getWardName());
        entity.setAddress(dto.getAddress());
        entity.setIdentityCard(dto.getIdentityCard());
        entity.setIdIssueDate(dto.getIdIssueDate());
        entity.setIdIssuePlace(dto.getIdIssuePlace());
        entity.setCustomerGroupId(dto.getCustomerGroupId());
        entity.setNote(dto.getNote());

        entity.setRepresentativeName(dto.getRepresentativeName());
        entity.setRepIdentityCard(dto.getRepIdentityCard());
        entity.setPosition(dto.getPosition());
        entity.setContractNumber(dto.getContractNumber());
        entity.setContractDate(dto.getContractDate());
        entity.setBankAccountInfo(dto.getBankAccountInfo());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());

        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setContactEmail(dto.getContactEmail());
        entity.setContactAddress(dto.getContactAddress());
    }

    @Override
    public void ensureCustomerCodeAvailable(String customerCode, Integer selfCustomerId) {
        if (customerCode == null || customerCode.isBlank()) return;
        customerProfileJpaRepo.findByCustomerCodeIgnoreCase(customerCode.trim()).ifPresent(other -> {
            if (!other.getCustomerId().equals(selfCustomerId)) {
                throw new CustomerException("Mã khách hàng đã tồn tại: " + customerCode,
                        CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
            }
        });
    }

    /** Nạp tên nhóm khách hàng để hiển thị trên danh bạ. */
    private void fillGroupName(CustomerProfile profile) {
        if (profile == null || profile.getCustomerGroupId() == null) return;
        customerGroupJpaRepo.findById(profile.getCustomerGroupId())
                .ifPresent(group -> profile.setCustomerGroupName(group.getName()));
    }

    @Override
    public CustomerAuth createNewCustomerAuth(CustomerCreateDto customerCreateDto, CustomerProfile customerProfile) {
        if (customerProfile.getCustomerId()==null){throw new CustomerException("Customer Id must not null!", CustomerErrorCode.INVALID_CUSTOMER_PROFILE);}
        
        CustomerAuthJpa customerAuthJpa = customerAuthJpaRepo.findByCustomerId(customerProfile.getCustomerId());
        if (customerAuthJpa == null) {
            customerAuthJpa = new CustomerAuthJpa();
            customerAuthJpa.setCustomerId(customerProfile.getCustomerId());
            customerAuthJpa.setCreatedAt(LocalDateTime.now());
        }
        
        customerAuthJpa.setStatus(CustomerStatus.ACTIVE);
        customerAuthJpa.setPinHash(passwordEncoder.encode(customerCreateDto.getPin()));
        return customerAuthJpaMapper.toJpa(customerAuthJpaRepo.save(customerAuthJpa));
    }

    @Override
    public Page<CustomerProfile> getListOfCustomers(int page,int size, LocalDate date, Boolean isGuest, String search, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<CustomerProfileJpa> specification = Specification.unrestricted();
        specification = specification.and(CustomerProfileSpecification.filter(date,status));
        if (search != null && !search.isBlank()) {
            specification = specification.and(CustomerProfileSpecification.searchProfiles(search));
        }
        Page<CustomerProfileJpa> customerProfileJpas = customerProfileJpaRepo.findAll(specification, pageable);
        // Nạp sẵn nhóm khách hàng một lần để tránh truy vấn lặp trên từng dòng.
        java.util.Map<Integer, String> groupNames = customerGroupJpaRepo.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.g42.platform.gms.customer.infrastructure.entity.CustomerGroupJpa::getGroupId,
                        com.g42.platform.gms.customer.infrastructure.entity.CustomerGroupJpa::getName));
        return customerProfileJpas.map(jpa -> {
            CustomerProfile profile = customerJpaMapper.toDomain(jpa);
            if (profile.getCustomerGroupId() != null) {
                profile.setCustomerGroupName(groupNames.get(profile.getCustomerGroupId()));
            }
            CustomerAuthJpa auth = customerAuthJpaRepo.findByCustomerId(jpa.getCustomerId());
            if (auth != null) profile.setStatus(auth.getStatus());
            // Default BRONZE nếu chưa có record điểm
            profile.setCurrentRank(com.g42.platform.gms.customer.domain.enums.CustomerRank.BRONZE);
            profile.setTotalPoints(0);
            profile.setTotalBookings(0);
            profile.setCurrentDealerRank("LEVEL_1");
            customerPointsJpaRepo.findByCustomerId(jpa.getCustomerId()).ifPresent(pts -> {
                profile.setCurrentRank(pts.getCurrentRank());
                profile.setTotalPoints(pts.getTotalPoints());
                if (pts.getCurrentDealerRank() != null) {
                    profile.setCurrentDealerRank(pts.getCurrentDealerRank().name());
                }
            });
            // Tính số lần đặt lịch
            long bookings = customerPointsHistoryJpaRepo.countByCustomerIdAndReason(jpa.getCustomerId(), "SERVICE_PAYMENT");
            profile.setTotalBookings((int) bookings);
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
        CustomerProfileJpa jpa = customerProfileJpaRepo.findByCustomerId(customerId);
        CustomerProfile profile = customerJpaMapper.toDomain(jpa);
        fillGroupName(profile);
        CustomerAuthJpa auth = customerAuthJpaRepo.findByCustomerId(customerId);
        if (auth != null) profile.setStatus(auth.getStatus());
        // Default BRONZE nếu chưa có record điểm
        profile.setCurrentRank(com.g42.platform.gms.customer.domain.enums.CustomerRank.BRONZE);
        profile.setTotalPoints(0);
        profile.setTotalBookings(0);
        profile.setCurrentDealerRank("LEVEL_1");
        customerPointsJpaRepo.findByCustomerId(customerId).ifPresent(pts -> {
            profile.setCurrentRank(pts.getCurrentRank());
            profile.setTotalPoints(pts.getTotalPoints());
            if (pts.getCurrentDealerRank() != null) {
                profile.setCurrentDealerRank(pts.getCurrentDealerRank().name());
            }
        });
        // Tính số lần đặt lịch
        long bookings = customerPointsHistoryJpaRepo.countByCustomerIdAndReason(customerId, "SERVICE_PAYMENT");
        profile.setTotalBookings((int) bookings);
        return profile;
    }
}
