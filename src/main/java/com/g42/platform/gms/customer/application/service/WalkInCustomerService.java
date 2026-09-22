package com.g42.platform.gms.customer.application.service;

import com.g42.platform.gms.customer.domain.enums.CustomerType;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerProfileJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Hồ sơ dùng chung cho khách lẻ vãng lai (bán hàng không lấy thông tin khách).
 *
 * Báo giá, hoá đơn và phiếu dịch vụ đều bắt buộc có customer_id, nhưng khách vãng
 * lai không có định danh (SĐT/email) nên không thể tạo hồ sơ thật cho từng lượt bán:
 * làm vậy danh bạ sẽ đầy hồ sơ chỉ-có-tên, trùng nhau mà không phân biệt được ai với
 * ai, đồng thời /customer-merge gợi ý gộp nhầm và hạng khách mất ý nghĩa.
 *
 * Thay vào đó cả hệ thống dùng đúng MỘT hồ sơ "Khách lẻ" làm chỗ neo kỹ thuật, còn
 * tên/SĐT/địa chỉ của từng lượt bán được chụp lại ngay trên phiếu
 * (service_ticket.walk_in_*, xem PartsSaleService).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalkInCustomerService {

    /** Mã hồ sơ dùng chung — tra theo mã nên đổi tên hiển thị không làm hỏng liên kết. */
    public static final String WALK_IN_CUSTOMER_CODE = "KHACH_LE";
    private static final String WALK_IN_CUSTOMER_NAME = "Khách lẻ";

    private final CustomerProfileJpaRepo customerProfileJpaRepo;

    /**
     * Hồ sơ "Khách lẻ", tự tạo ở lần bán đầu tiên.
     *
     * Hồ sơ cố ý không có SĐT/email: nó không phải một người thật nên không được
     * chiếm định danh của ai, và cũng không nhận thông báo Zalo/email.
     */
    @Transactional
    public CustomerProfileJpa getOrCreateWalkInCustomer() {
        return customerProfileJpaRepo.findByCustomerCodeIgnoreCase(WALK_IN_CUSTOMER_CODE)
                .orElseGet(this::createWalkInCustomer);
    }

    private CustomerProfileJpa createWalkInCustomer() {
        CustomerProfileJpa profile = new CustomerProfileJpa();
        profile.setCustomerCode(WALK_IN_CUSTOMER_CODE);
        profile.setFullName(WALK_IN_CUSTOMER_NAME);
        profile.setCustomerType(CustomerType.INDIVIDUAL);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setNote("Hồ sơ hệ thống dùng chung cho các phiếu bán lẻ khách vãng lai. "
                + "Không sửa, không gộp, không dùng cho khách có hồ sơ riêng.");
        CustomerProfileJpa saved = customerProfileJpaRepo.save(profile);
        log.info("Created shared walk-in customer profile (customerId={})", saved.getCustomerId());
        return saved;
    }
}
