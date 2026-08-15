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
        System.out.println("[DEBUG_PROFILE] phone=" + customerDto.getPhone());
        if (customerDto.getPhone()==null){
            throw new CustomerException("Phone must not null!", CustomerErrorCode.INVALID_PHONE);
        }
        
        // Kiểm tra xem khách hàng đã tồn tại chưa (khách vãng lai đã có profile nhưng chưa có tài khoản)
        CustomerProfileJpa entity = customerProfileJpaRepo.findByPhone(customerDto.getPhone());
        System.out.println("[DEBUG_PROFILE] found existing entity=" + (entity != null ? entity.getCustomerId() : "null"));
        if (entity == null) {
            entity = new CustomerProfileJpa();
        }
        
        entity.setFullName(customerDto.getFullName());
        entity.setPhone(customerDto.getPhone());
        ensureEmailAvailable(customerDto.getEmail(), entity.getCustomerId());
        entity.setEmail(emptyToNull(customerDto.getEmail()));
        entity.setGender(customerDto.getGender());
        entity.setAvatar(customerDto.getAvatar());
        if (customerDto.getNotificationChannel() != null) {
            entity.setNotificationChannel(customerDto.getNotificationChannel());
        }
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

        System.out.println("[DEBUG_PROFILE] calling applyPartnerFields");
        applyPartnerFields(entity, customerDto);

        System.out.println("[DEBUG_PROFILE] calling save");
        CustomerProfileJpa saved = customerProfileJpaRepo.save(entity);
        System.out.println("[DEBUG_PROFILE] saved customerId=" + saved.getCustomerId());
        // Mã khách hàng để trống thì sinh tự động theo id (KH00001, KH00002, ...)
        if (saved.getCustomerCode() == null || saved.getCustomerCode().isBlank()) {
            saved.setCustomerCode(String.format("KH%05d", saved.getCustomerId()));
            saved = customerProfileJpaRepo.save(saved);
        }
        System.out.println("[DEBUG_PROFILE] customerCode=" + saved.getCustomerCode() + ", isCompany=" + saved.getIsCompany());
        CustomerProfile domain = customerJpaMapper.toDomain(saved);
        copyPartnerFieldsToDomain(saved, domain);
        System.out.println("[DEBUG_PROFILE] done, domain.isCompany=" + domain.getIsCompany());
        return domain;
    }

    /** Gán các trường mở rộng của Danh bạ đối tác từ DTO tạo mới. */
    private void applyPartnerFields(CustomerProfileJpa entity, CustomerCreateDto dto) {
        String code = dto.getCustomerCode() == null ? null : dto.getCustomerCode().trim();
        if (code != null && !code.isEmpty()) {
            ensureCustomerCodeAvailable(code, entity.getCustomerId());
            entity.setCustomerCode(code);
        }

        if (dto.getIsCompany() != null) {
            entity.setIsCompany(dto.getIsCompany());
        } else {
            entity.setIsCompany(false);
        }
        
        if (Boolean.TRUE.equals(entity.getIsCompany())) {
            entity.setCompanyName(dto.getCompanyName());
        } else {
            entity.setCompanyName(null);
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

    /** Email rỗng lưu thành NULL để nhiều hồ sơ "chưa có email" không vướng ràng buộc duy nhất. */
    private String emptyToNull(String email) {
        return (email == null || email.isBlank()) ? null : email.trim();
    }

    @Override
    public void ensureEmailAvailable(String email, Integer selfCustomerId) {
        if (email == null || email.isBlank()) return;
        customerProfileJpaRepo.findByEmailIgnoreCase(email.trim()).ifPresent(other -> {
            if (!other.getCustomerId().equals(selfCustomerId)) {
                throw new CustomerException("Email đã được dùng cho khách hàng khác: " + email,
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
            System.out.println("[DEBUG_JAVA_LIST] ID=" + jpa.getCustomerId() + " Name=" + jpa.getFullName() + " jpaIsCompany=" + jpa.getIsCompany() + " jpaCompanyName=" + jpa.getCompanyName());
            CustomerProfile profile = customerJpaMapper.toDomain(jpa);
            copyPartnerFieldsToDomain(jpa, profile);
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

        CustomerProfile profile = customerJpaMapper.toDomain(customerProfileJpa);
        copyPartnerFieldsToDomain(customerProfileJpa, profile);
        return profile;
    }

    @Override
    public CustomerAuth findAuthById(Integer customerId) {
        CustomerAuthJpa customerAuthJpa = customerAuthJpaRepo.findByCustomerId(customerId);
        return customerAuthJpaMapper.toJpa(customerAuthJpa);
    }

    @Override
    public boolean updateCustomer(Integer customerId, CustomerProfile customerProfile, CustomerAuth customerAuth) {
        CustomerProfileJpa jpa = customerProfileJpaRepo.findByCustomerId(customerId);
        if (jpa == null) return false;
        
        jpa.setFullName(customerProfile.getFullName());
        jpa.setPhone(customerProfile.getPhone());
        ensureEmailAvailable(customerProfile.getEmail(), customerId);
        jpa.setEmail(emptyToNull(customerProfile.getEmail()));
        jpa.setDob(customerProfile.getDob());
        jpa.setGender(customerProfile.getGender());
        jpa.setAvatar(customerProfile.getAvatar());
        jpa.setCustomerType(customerProfile.getCustomerType());
        jpa.setIsDealer(customerProfile.getIsDealer());
        if (customerProfile.getNotificationChannel() != null) {
            jpa.setNotificationChannel(customerProfile.getNotificationChannel());
        }
        copyPartnerFieldsToJpa(customerProfile, jpa);
        
        CustomerProfileJpa savedProfile = customerProfileJpaRepo.save(jpa);

        CustomerAuthJpa authJpa = customerAuthJpaRepo.findByCustomerId(customerId);
        if (authJpa != null && customerAuth != null) {
            if (customerAuth.getStatus() != null) authJpa.setStatus(customerAuth.getStatus());
            if (customerAuth.getLastLoginAt() != null) authJpa.setLastLoginAt(customerAuth.getLastLoginAt());
            customerAuthJpaRepo.save(authJpa);
        }
        return savedProfile != null;
    }

    @Override
    public CustomerProfile findCustomerById(Integer customerId) {
        CustomerProfileJpa jpa = customerProfileJpaRepo.findByCustomerId(customerId);
        System.out.println("[DEBUG_JAVA_DETAIL] ID=" + customerId + " jpaIsCompany=" + (jpa != null ? jpa.getIsCompany() : "NULL_JPA") + " jpaCompanyName=" + (jpa != null ? jpa.getCompanyName() : "NULL_JPA"));
        CustomerProfile profile = customerJpaMapper.toDomain(jpa);
        copyPartnerFieldsToDomain(jpa, profile);
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

    private void copyPartnerFieldsToDomain(CustomerProfileJpa jpa, CustomerProfile domain) {
        if (jpa == null || domain == null) return;
        boolean isComp = Boolean.TRUE.equals(jpa.getIsCompany()) || (jpa.getCompanyName() != null && !jpa.getCompanyName().isBlank());
        domain.setIsCompany(isComp);
        domain.setCompanyName(jpa.getCompanyName() != null ? jpa.getCompanyName() : "");
        domain.setCustomerCode(jpa.getCustomerCode());
        domain.setTaxCode(jpa.getTaxCode());
        domain.setProvinceId(jpa.getProvinceId());
        domain.setProvinceName(jpa.getProvinceName());
        domain.setDistrictId(jpa.getDistrictId());
        domain.setDistrictName(jpa.getDistrictName());
        domain.setWardId(jpa.getWardId());
        domain.setWardName(jpa.getWardName());
        domain.setAddress(jpa.getAddress());
        domain.setIdentityCard(jpa.getIdentityCard());
        domain.setIdIssueDate(jpa.getIdIssueDate());
        domain.setIdIssuePlace(jpa.getIdIssuePlace());
        domain.setCustomerGroupId(jpa.getCustomerGroupId());
        domain.setNote(jpa.getNote());
        domain.setRepresentativeName(jpa.getRepresentativeName());
        domain.setRepIdentityCard(jpa.getRepIdentityCard());
        domain.setPosition(jpa.getPosition());
        domain.setContractNumber(jpa.getContractNumber());
        domain.setContractDate(jpa.getContractDate());
        domain.setBankAccountInfo(jpa.getBankAccountInfo());
        domain.setLatitude(jpa.getLatitude());
        domain.setLongitude(jpa.getLongitude());
        domain.setContactName(jpa.getContactName());
        domain.setContactPhone(jpa.getContactPhone());
        domain.setContactEmail(jpa.getContactEmail());
        domain.setContactAddress(jpa.getContactAddress());
    }

    private void copyPartnerFieldsToJpa(CustomerProfile domain, CustomerProfileJpa jpa) {
        if (domain == null || jpa == null) return;
        boolean isComp = Boolean.TRUE.equals(domain.getIsCompany()) || (domain.getCompanyName() != null && !domain.getCompanyName().isBlank());
        jpa.setIsCompany(isComp);
        jpa.setCompanyName(domain.getCompanyName());
        jpa.setCustomerCode(domain.getCustomerCode());
        jpa.setTaxCode(domain.getTaxCode());
        jpa.setProvinceId(domain.getProvinceId());
        jpa.setProvinceName(domain.getProvinceName());
        jpa.setDistrictId(domain.getDistrictId());
        jpa.setDistrictName(domain.getDistrictName());
        jpa.setWardId(domain.getWardId());
        jpa.setWardName(domain.getWardName());
        jpa.setAddress(domain.getAddress());
        jpa.setIdentityCard(domain.getIdentityCard());
        jpa.setIdIssueDate(domain.getIdIssueDate());
        jpa.setIdIssuePlace(domain.getIdIssuePlace());
        jpa.setCustomerGroupId(domain.getCustomerGroupId());
        jpa.setNote(domain.getNote());
        jpa.setRepresentativeName(domain.getRepresentativeName());
        jpa.setRepIdentityCard(domain.getRepIdentityCard());
        jpa.setPosition(domain.getPosition());
        jpa.setContractNumber(domain.getContractNumber());
        jpa.setContractDate(domain.getContractDate());
        jpa.setBankAccountInfo(domain.getBankAccountInfo());
        jpa.setLatitude(domain.getLatitude());
        jpa.setLongitude(domain.getLongitude());
        jpa.setContactName(domain.getContactName());
        jpa.setContactPhone(domain.getContactPhone());
        jpa.setContactEmail(domain.getContactEmail());
        jpa.setContactAddress(domain.getContactAddress());
    }
}
