package com.g42.platform.gms.customer.application.service;

import com.g42.platform.gms.customer.api.dto.CustomerPhonesDto;
import com.g42.platform.gms.customer.domain.exception.CustomerErrorCode;
import com.g42.platform.gms.customer.domain.exception.CustomerException;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerPhoneJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerPhoneJpaRepo;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerProfileJpaRepo;
import com.g42.platform.gms.customerimport.application.service.ImportNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Một khách nhiều số điện thoại (changeset 037).
 *
 * Số chính vẫn ở customer_profile.phone — là số đăng nhập, là subject của JWT khách, là số
 * nhận Zalo — nên hàng chục chỗ đang đọc cột đó không phải sửa. Số phụ ở customer_phone.
 * Quy tắc bất biến: một số chỉ thuộc về MỘT khách, tính trên cả hai bảng (DB chỉ chặn được
 * trùng trong từng bảng, phần chéo bảng do lớp này chặn).
 */
@Service
@RequiredArgsConstructor
public class CustomerPhoneService {

    private final CustomerProfileJpaRepo profileRepo;
    private final CustomerPhoneJpaRepo phoneRepo;

    @Transactional(readOnly = true)
    public CustomerPhonesDto getPhones(Integer customerId) {
        CustomerProfileJpa profile = requireProfile(customerId);
        CustomerPhonesDto dto = new CustomerPhonesDto();
        dto.setCustomerId(customerId);
        dto.setPrimaryPhone(profile.getPhone());
        for (CustomerPhoneJpa phone : phoneRepo.findByCustomerIdOrderByCustomerPhoneIdAsc(customerId)) {
            CustomerPhonesDto.OtherPhone other = new CustomerPhonesDto.OtherPhone();
            other.setCustomerPhoneId(phone.getCustomerPhoneId());
            other.setPhone(phone.getPhone());
            other.setNote(phone.getNote());
            dto.getOtherPhones().add(other);
        }
        return dto;
    }

    /**
     * Ghi đè toàn bộ danh sách số của khách. Không có số chính mà có số phụ thì số phụ đầu
     * tiên được đôn lên làm số chính.
     */
    @Transactional
    public CustomerPhonesDto replacePhones(Integer customerId, CustomerPhonesDto request) {
        CustomerProfileJpa profile = requireProfile(customerId);

        String primary = normalizeNew(request.getPrimaryPhone(), profile.getPhone());
        List<CustomerPhonesDto.OtherPhone> others = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (primary != null) seen.add(primary);

        for (CustomerPhonesDto.OtherPhone item : request.getOtherPhones() == null
                ? List.<CustomerPhonesDto.OtherPhone>of() : request.getOtherPhones()) {
            String phone = normalizeNew(item.getPhone(), null);
            if (phone == null) continue;
            if (!seen.add(phone)) {
                throw new CustomerException("Số " + phone + " bị nhập hai lần trong danh sách.",
                        CustomerErrorCode.DUPLICATE_PHONE);
            }
            if (primary == null) {
                primary = phone;
                continue;
            }
            CustomerPhonesDto.OtherPhone normalized = new CustomerPhonesDto.OtherPhone();
            normalized.setPhone(phone);
            normalized.setNote(trimToNull(item.getNote(), 100));
            others.add(normalized);
        }

        for (String phone : seen) ensurePhoneFree(phone, customerId);

        phoneRepo.deleteByCustomerId(customerId);
        phoneRepo.flush();

        profile.setPhone(primary);
        profileRepo.saveAndFlush(profile);

        LocalDateTime now = LocalDateTime.now();
        for (CustomerPhonesDto.OtherPhone other : others) {
            CustomerPhoneJpa entity = new CustomerPhoneJpa();
            entity.setCustomerId(customerId);
            entity.setPhone(other.getPhone());
            entity.setNote(other.getNote());
            entity.setCreatedAt(now);
            phoneRepo.save(entity);
        }
        phoneRepo.flush();
        return getPhones(customerId);
    }

    /**
     * Gọi khi số chính của khách vừa đổi qua màn sửa hồ sơ: nếu số mới đang là số phụ của
     * chính khách này thì bỏ khỏi danh sách phụ để không tồn tại hai nơi.
     */
    @Transactional
    public void detachOtherPhone(Integer customerId, String phone) {
        if (phone == null || phone.isBlank()) return;
        phoneRepo.findByPhone(phone.trim()).ifPresent(existing -> {
            if (existing.getCustomerId().equals(customerId)) {
                phoneRepo.delete(existing);
                phoneRepo.flush();
            }
        });
    }

    /** Chặn số đã thuộc về khách khác — tính cả số chính lẫn số phụ của họ. */
    public void ensurePhoneFree(String phone, Integer selfCustomerId) {
        if (phone == null || phone.isBlank()) return;
        CustomerProfileJpa owner = profileRepo.findByPhone(phone.trim());
        if (owner != null && !owner.getCustomerId().equals(selfCustomerId)) {
            String name = owner.getFullName() == null || owner.getFullName().isBlank()
                    ? "chưa có tên" : owner.getFullName();
            String code = owner.getCustomerCode() == null ? "" : " (" + owner.getCustomerCode() + ")";
            throw new CustomerException("Số điện thoại " + phone + " đã thuộc về khách hàng " + name + code
                    + ". Nếu đó là cùng một người, hãy gộp hai hồ sơ ở màn Gộp hồ sơ trùng.",
                    CustomerErrorCode.DUPLICATE_PHONE);
        }
    }

    /**
     * Chuẩn hoá số vừa nhập. Số chính giữ nguyên như cũ thì không bắt đúng định dạng (hồ sơ
     * cũ có thể lưu số lạ); số mới thì phải là 10 số bắt đầu bằng 0.
     */
    private String normalizeNew(String raw, String unchangedValue) {
        if (raw == null || raw.isBlank()) return null;
        if (unchangedValue != null && raw.trim().equals(unchangedValue)) return unchangedValue;
        String phone = ImportNormalizer.normalizePhone(raw);
        if (!ImportNormalizer.isValidPhone(phone)) {
            throw new CustomerException("Số điện thoại \"" + raw + "\" không đúng dạng 10 số bắt đầu bằng 0.",
                    CustomerErrorCode.INVALID_PHONE);
        }
        return phone;
    }

    private CustomerProfileJpa requireProfile(Integer customerId) {
        CustomerProfileJpa profile = profileRepo.findByCustomerId(customerId);
        if (profile == null) {
            throw new CustomerException("Không tìm thấy khách hàng " + customerId, CustomerErrorCode.INVALID_ID);
        }
        return profile;
    }

    private static String trimToNull(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
