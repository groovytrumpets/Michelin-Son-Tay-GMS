package com.g42.platform.gms.customer.application.service;


import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.api.dto.CustomerDuplicateCheckDto;
import com.g42.platform.gms.customer.api.dto.CustomerUpdateDto;
import com.g42.platform.gms.customer.api.mapper.CustomerDtoMapper;
import com.g42.platform.gms.customer.domain.entity.CustomerAuth;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.customer.domain.exception.CustomerErrorCode;
import com.g42.platform.gms.customer.domain.exception.CustomerException;
import com.g42.platform.gms.customer.domain.repository.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CustomerService {
    @Autowired
    CustomerRepo customerRepo;
    @Autowired
    CustomerDtoMapper customerDtoMapper;
    @Autowired
    CustomerRankingService customerRankingService;
    @Autowired
    CustomerPhoneService customerPhoneService;
    private PasswordEncoder passwordEncoder;
    
    @Transactional
    public CustomerCreateDto createNewCustomer(CustomerCreateDto customerDto) {
        try {
            System.out.println("[DEBUG_CREATE] Step 1: Creating profile for phone=" + customerDto.getPhone());
            CustomerProfile customerProfile = customerRepo.createNewCustomerProfile(customerDto);
            System.out.println("[DEBUG_CREATE] Step 2: Profile created, customerId=" + customerProfile.getCustomerId());
            CustomerAuth customerAuth = customerRepo.createNewCustomerAuth(customerDto, customerProfile);
            System.out.println("[DEBUG_CREATE] Step 3: Auth created, returning DTO");
            return customerDtoMapper.toCusCreateDto(customerProfile, customerAuth);
        } catch (Exception e) {
            System.err.println("[DEBUG_CREATE] FAILED at: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("[DEBUG_CREATE] Cause: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            e.printStackTrace();
            throw e;
        }
    }

    /** Tra trùng định danh khách trước khi tạo hồ sơ mới — SĐT và email phải là duy nhất. */
    public CustomerDuplicateCheckDto checkDuplicate(String phone, String email, Integer excludeCustomerId) {
        return customerRepo.checkDuplicate(phone, email, excludeCustomerId);
    }

    public Page<CustomerProfile> getListOfAllCustomerProfile(int page, int size, LocalDate date, Boolean isGuest, String search, String status) {
        return customerRepo.getListOfCustomers(page,size,date,isGuest,search,status);
    }
    @Transactional
    public CustomerCreateDto updateCustomer(Integer customerId, CustomerUpdateDto customerUpdateDto) {
        CustomerProfile customerProfile = customerRepo.findProflieById(customerId);
        CustomerAuth customerAuth = customerRepo.findAuthById(customerId);
        if (customerUpdateDto.getFullName() != null) customerProfile.setFullName(customerUpdateDto.getFullName());
        if (customerUpdateDto.getPhone() != null && !customerUpdateDto.getPhone().equals(customerProfile.getPhone())) {
            // Một số chỉ thuộc một khách (tính cả số phụ). Số mới đang là số phụ của chính khách
            // này thì coi như đôn lên làm số chính.
            String newPhone = customerUpdateDto.getPhone().isBlank() ? null : customerUpdateDto.getPhone().trim();
            customerPhoneService.ensurePhoneFree(newPhone, customerId);
            customerPhoneService.detachOtherPhone(customerId, newPhone);
            customerProfile.setPhone(newPhone);
        }
        if (customerUpdateDto.getEmail() != null) customerProfile.setEmail(customerUpdateDto.getEmail());
        if (customerUpdateDto.getGender() != null) customerProfile.setGender(customerUpdateDto.getGender());
        if (customerUpdateDto.getAvatar() != null) customerProfile.setAvatar(customerUpdateDto.getAvatar());
        if (customerUpdateDto.getCustomerType() != null) {
            customerProfile.setCustomerType(customerUpdateDto.getCustomerType());
        }
        if (customerUpdateDto.getIsDealer() != null) {
            customerProfile.setIsDealer(customerUpdateDto.getIsDealer());
        }
        if (customerUpdateDto.getNotificationChannel() != null) {
            customerProfile.setNotificationChannel(customerUpdateDto.getNotificationChannel());
        }
        if (customerUpdateDto.getStatus() != null) {
            customerAuth.setStatus(customerUpdateDto.getStatus());
        }
        if (customerUpdateDto.getLastLoginAt() != null) {
            customerAuth.setLastLoginAt(customerUpdateDto.getLastLoginAt());
        }
        applyPartnerFields(customerProfile, customerUpdateDto, customerId);
        if (!customerRepo.updateCustomer(customerId,customerProfile,customerAuth)){
            throw new CustomerException("Update fail!", CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
        }
        return customerDtoMapper.toCusCreateDto(customerProfile,customerAuth);
    }

    /**
     * Cập nhật các trường mở rộng của Danh bạ đối tác.
     * Chỉ ghi đè khi client thực sự gửi giá trị (null = giữ nguyên) để các màn
     * hình cũ chỉ gửi vài trường không xoá mất dữ liệu đối tác.
     */
    private void applyPartnerFields(CustomerProfile profile, CustomerUpdateDto dto, Integer customerId) {
        if (dto.getCustomerCode() != null) {
            String code = dto.getCustomerCode().trim();
            if (!code.isEmpty()) {
                customerRepo.ensureCustomerCodeAvailable(code, customerId);
                profile.setCustomerCode(code);
            }
        }
        
        if (dto.getIsCompany() != null) {
            profile.setIsCompany(dto.getIsCompany());
        }
        if (Boolean.TRUE.equals(profile.getIsCompany()) && dto.getCompanyName() != null) {
            profile.setCompanyName(dto.getCompanyName());
        } else if (Boolean.FALSE.equals(profile.getIsCompany())) {
            profile.setCompanyName(null);
        }

        if (dto.getTaxCode() != null) profile.setTaxCode(dto.getTaxCode());
        if (dto.getProvinceId() != null) profile.setProvinceId(emptyToNull(dto.getProvinceId()));
        if (dto.getProvinceName() != null) profile.setProvinceName(emptyToNull(dto.getProvinceName()));
        if (dto.getDistrictId() != null) profile.setDistrictId(emptyToNull(dto.getDistrictId()));
        if (dto.getDistrictName() != null) profile.setDistrictName(emptyToNull(dto.getDistrictName()));
        if (dto.getWardId() != null) profile.setWardId(emptyToNull(dto.getWardId()));
        if (dto.getWardName() != null) profile.setWardName(emptyToNull(dto.getWardName()));
        if (dto.getAddress() != null) profile.setAddress(dto.getAddress());
        if (dto.getIdentityCard() != null) profile.setIdentityCard(dto.getIdentityCard());
        if (dto.getIdIssueDate() != null) profile.setIdIssueDate(dto.getIdIssueDate());
        if (dto.getIdIssuePlace() != null) profile.setIdIssuePlace(dto.getIdIssuePlace());
        if (dto.getCustomerGroupId() != null) profile.setCustomerGroupId(dto.getCustomerGroupId());
        if (dto.getNote() != null) profile.setNote(dto.getNote());

        if (dto.getRepresentativeName() != null) profile.setRepresentativeName(dto.getRepresentativeName());
        if (dto.getRepIdentityCard() != null) profile.setRepIdentityCard(dto.getRepIdentityCard());
        if (dto.getPosition() != null) profile.setPosition(dto.getPosition());
        if (dto.getContractNumber() != null) profile.setContractNumber(dto.getContractNumber());
        if (dto.getContractDate() != null) profile.setContractDate(dto.getContractDate());
        if (dto.getBankAccountInfo() != null) profile.setBankAccountInfo(dto.getBankAccountInfo());
        if (dto.getLatitude() != null) profile.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) profile.setLongitude(dto.getLongitude());

        if (dto.getContactName() != null) profile.setContactName(dto.getContactName());
        if (dto.getContactPhone() != null) profile.setContactPhone(dto.getContactPhone());
        if (dto.getContactEmail() != null) profile.setContactEmail(dto.getContactEmail());
        if (dto.getContactAddress() != null) profile.setContactAddress(dto.getContactAddress());
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public CustomerProfile findByCustomerId(Integer customerId) {
        return customerRepo.findCustomerById(customerId);
    }

    /**
     * Xóa mềm (không ai được khôi phục trừ can thiệp DB trực tiếp). Xóa nghĩa là giải phóng
     * SĐT/email để dùng lại — nếu để nguyên, hồ sơ đã xóa vẫn chiếm định danh và chặn tạo mới
     * vĩnh viễn (ensurePhoneAvailable/ensureEmailAvailable không loại trừ khách DELETED, và
     * email còn có ràng buộc UNIQUE thật ở DB nên không thể chỉ bỏ qua ở tầng service).
     */
    public CustomerProfile deleteCustomer(Integer customerId) {
        CustomerProfile customerProfile = customerRepo.findProflieById(customerId);
        CustomerAuth customerAuth = customerRepo.findAuthById(customerId);
        customerAuth.setStatus(CustomerStatus.DELETED);
        customerProfile.setPhone(null);
        customerProfile.setEmail(null);
        if (!customerRepo.updateCustomer(customerId,customerProfile,customerAuth)){
            throw new CustomerException("Update fail!", CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
        }
        return findByCustomerId(customerId);
    }
    /**
     * Nhân viên kích hoạt hộ tài khoản khách.
     *
     * Dành cho khách nhập từ sổ Excel cũ: hồ sơ được tạo ở trạng thái INACTIVE nên
     * chưa đăng nhập được, PIN khởi tạo là 6 số cuối số điện thoại. Kích hoạt ở đây
     * GIỮ NGUYÊN mã PIN đó — cờ must_change_pin đã bật từ lúc nhập nên khách buộc phải
     * đổi ở lần đăng nhập đầu, vì ai biết số điện thoại là đoán được PIN.
     *
     * Khách tự kích hoạt qua OTP Zalo thì đi đường khác (CustomerAuthService.setupPin)
     * và tự đặt PIN mới, an toàn hơn — đây chỉ là lối phụ khi khách không tự làm được.
     */
    @Transactional
    public CustomerProfile activateCustomer(Integer customerId) {
        CustomerProfile customerProfile = customerRepo.findProflieById(customerId);
        CustomerAuth customerAuth = customerRepo.findAuthById(customerId);
        if (customerAuth == null) {
            throw new CustomerException("Khách chưa có bản ghi bảo mật, không kích hoạt được.",
                    CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
        }
        if (customerAuth.getPinHash() == null || customerAuth.getPinHash().isBlank()) {
            throw new CustomerException(
                    "Tài khoản chưa có mã PIN. Khách cần tự đặt PIN qua xác thực OTP Zalo.",
                    CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
        }
        if (customerAuth.getStatus() == CustomerStatus.LOCKED) {
            throw new CustomerException("Tài khoản đang bị khóa, mở khóa trước khi kích hoạt.",
                    CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
        }
        customerAuth.setStatus(CustomerStatus.ACTIVE);
        if (!customerRepo.updateCustomer(customerId, customerProfile, customerAuth)) {
            throw new CustomerException("Update fail!", CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
        }
        return findByCustomerId(customerId);
    }

    public CustomerProfile lockedCustomer(Integer customerId) {
        CustomerProfile customerProfile = customerRepo.findProflieById(customerId);
        CustomerAuth customerAuth = customerRepo.findAuthById(customerId);
        customerAuth.setStatus(CustomerStatus.LOCKED);
        if (!customerRepo.updateCustomer(customerId,customerProfile,customerAuth)){
            throw new CustomerException("Update fail!", CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
        }
        return findByCustomerId(customerId);
    }
}
