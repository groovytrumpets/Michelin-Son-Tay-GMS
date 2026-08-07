package com.g42.platform.gms.customer.application.service;

import com.g42.platform.gms.customer.api.dto.CustomerGroupDto;
import com.g42.platform.gms.customer.domain.exception.CustomerErrorCode;
import com.g42.platform.gms.customer.domain.exception.CustomerException;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerGroupJpa;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerGroupJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Quản lý nhóm khách hàng (Danh bạ đối tác).
 */
@Service
@RequiredArgsConstructor
public class CustomerGroupService {

    private final CustomerGroupJpaRepo customerGroupJpaRepo;

    public List<CustomerGroupDto> findAll(boolean activeOnly) {
        List<CustomerGroupJpa> groups = activeOnly
                ? customerGroupJpaRepo.findByActiveTrueOrderByNameAsc()
                : customerGroupJpaRepo.findAllByOrderByNameAsc();
        return groups.stream().map(this::toDto).toList();
    }

    @Transactional
    public CustomerGroupDto create(CustomerGroupDto dto) {
        String name = dto.getName() == null ? "" : dto.getName().trim();
        if (name.isEmpty()) {
            throw new CustomerException("Tên nhóm khách hàng không được để trống!",
                    CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
        }

        String code = dto.getCode() == null || dto.getCode().isBlank()
                ? generateCode(name)
                : dto.getCode().trim().toUpperCase(Locale.ROOT);
        if (customerGroupJpaRepo.findByCodeIgnoreCase(code).isPresent()) {
            throw new CustomerException("Mã nhóm khách hàng đã tồn tại: " + code,
                    CustomerErrorCode.INVALID_CUSTOMER_PROFILE);
        }

        CustomerGroupJpa entity = new CustomerGroupJpa();
        entity.setCode(code);
        entity.setName(name);
        entity.setNote(dto.getNote());
        entity.setActive(dto.getActive() == null || dto.getActive());
        entity.setCreatedAt(LocalDateTime.now());
        return toDto(customerGroupJpaRepo.save(entity));
    }

    @Transactional
    public CustomerGroupDto update(Integer groupId, CustomerGroupDto dto) {
        CustomerGroupJpa entity = customerGroupJpaRepo.findById(groupId)
                .orElseThrow(() -> new CustomerException("Không tìm thấy nhóm khách hàng!",
                        CustomerErrorCode.INVALID_CUSTOMER_PROFILE));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            entity.setName(dto.getName().trim());
        }
        if (dto.getNote() != null) {
            entity.setNote(dto.getNote());
        }
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }
        return toDto(customerGroupJpaRepo.save(entity));
    }

    @Transactional
    public void delete(Integer groupId) {
        CustomerGroupJpa entity = customerGroupJpaRepo.findById(groupId)
                .orElseThrow(() -> new CustomerException("Không tìm thấy nhóm khách hàng!",
                        CustomerErrorCode.INVALID_CUSTOMER_PROFILE));
        // Ngừng sử dụng thay vì xoá cứng để không làm hỏng hồ sơ đang tham chiếu.
        entity.setActive(false);
        customerGroupJpaRepo.save(entity);
    }

    /** Tên nhóm -> mã viết hoa không dấu, ví dụ "Đại lý vùng" -> "DAI_LY_VUNG". */
    private String generateCode(String name) {
        String noAccent = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        String base = noAccent.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
        if (base.isEmpty()) base = "GROUP";
        if (base.length() > 40) base = base.substring(0, 40);

        String candidate = base;
        int suffix = 2;
        while (customerGroupJpaRepo.findByCodeIgnoreCase(candidate).isPresent()) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private CustomerGroupDto toDto(CustomerGroupJpa entity) {
        return new CustomerGroupDto(
                entity.getGroupId(),
                entity.getCode(),
                entity.getName(),
                entity.getNote(),
                entity.getActive()
        );
    }
}
