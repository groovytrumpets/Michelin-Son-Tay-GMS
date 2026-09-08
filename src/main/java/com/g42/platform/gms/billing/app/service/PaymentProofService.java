package com.g42.platform.gms.billing.app.service;

import com.g42.platform.gms.billing.api.dto.PaymentProofCreateDto;
import com.g42.platform.gms.billing.api.dto.PaymentProofDto;
import com.g42.platform.gms.billing.infrastructure.entity.PaymentProofJpa;
import com.g42.platform.gms.billing.infrastructure.repository.PaymentProofJpaRepo;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Quản lý chứng từ thanh toán (ảnh/video) của một phiếu dịch vụ.
 *
 * <p>File đã được FE đẩy lên Cloudinary; service này chỉ lưu/liệt kê/xoá bản ghi
 * URL và ép giới hạn {@value #MAX_PROOFS_PER_TICKET} file cho mỗi phiếu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProofService {

    /** Giới hạn số chứng từ cho mỗi phiếu dịch vụ. */
    public static final int MAX_PROOFS_PER_TICKET = 10;

    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of("IMAGE", "VIDEO");

    private final PaymentProofJpaRepo paymentProofJpaRepo;
    private final ServiceTicketRepo serviceTicketRepo;
    private final ImageUploadService imageUploadService;

    @Transactional(readOnly = true)
    public List<PaymentProofDto> list(Integer serviceTicketId) {
        requireTicket(serviceTicketId);
        return paymentProofJpaRepo
                .findByServiceTicketIdOrderByCreatedAtAscPaymentProofIdAsc(serviceTicketId)
                .stream()
                .map(PaymentProofDto::from)
                .toList();
    }

    @Transactional
    public List<PaymentProofDto> add(Integer serviceTicketId, PaymentProofCreateDto dto, Integer staffId) {
        requireTicket(serviceTicketId);

        List<PaymentProofCreateDto.Item> items = dto == null ? null : dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Chưa có chứng từ nào để lưu.");
        }

        long existing = paymentProofJpaRepo.countByServiceTicketId(serviceTicketId);
        if (existing + items.size() > MAX_PROOFS_PER_TICKET) {
            throw new IllegalArgumentException(
                    "Mỗi phiếu chỉ được tối đa " + MAX_PROOFS_PER_TICKET + " chứng từ (hiện có " + existing + ").");
        }

        for (PaymentProofCreateDto.Item item : items) {
            String url = item.getUrl() == null ? "" : item.getUrl().trim();
            if (url.isEmpty()) {
                throw new IllegalArgumentException("Chứng từ thiếu đường dẫn file.");
            }
            String mediaType = normalizeMediaType(item.getMediaType());

            PaymentProofJpa entity = new PaymentProofJpa();
            entity.setServiceTicketId(serviceTicketId);
            entity.setBillId(dto.getBillId());
            entity.setMediaType(mediaType);
            entity.setUrl(url);
            entity.setPublicId(trimToNull(item.getPublicId()));
            entity.setFileName(trimToNull(item.getFileName()));
            entity.setFileSize(item.getFileSize());
            entity.setNote(trimToNull(item.getNote()));
            entity.setUploadedBy(staffId);
            paymentProofJpaRepo.save(entity);
        }

        return list(serviceTicketId);
    }

    @Transactional
    public void delete(Integer serviceTicketId, Integer paymentProofId) {
        requireTicket(serviceTicketId);
        PaymentProofJpa entity = paymentProofJpaRepo
                .findByPaymentProofIdAndServiceTicketId(paymentProofId, serviceTicketId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chứng từ."));

        // Dọn file trên Cloudinary tốt nhất có thể — ảnh dùng API sẵn có; video/raw
        // để lại vì common service chưa có hàm xoá riêng, không chặn xoá bản ghi.
        if ("IMAGE".equals(entity.getMediaType()) && entity.getPublicId() != null) {
            try {
                imageUploadService.deleteImage(entity.getPublicId());
            } catch (Exception ex) {
                log.warn("Không xoá được ảnh chứng từ trên Cloudinary (publicId={}): {}",
                        entity.getPublicId(), ex.getMessage());
            }
        }

        paymentProofJpaRepo.delete(entity);
    }

    private void requireTicket(Integer serviceTicketId) {
        if (serviceTicketId == null || serviceTicketId <= 0) {
            throw new IllegalArgumentException("Thiếu serviceTicketId hợp lệ.");
        }
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu dịch vụ: " + serviceTicketId);
        }
    }

    private String normalizeMediaType(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase();
        if (value.isEmpty()) {
            return "IMAGE";
        }
        if (!ALLOWED_MEDIA_TYPES.contains(value)) {
            throw new IllegalArgumentException("Loại chứng từ không hợp lệ: " + raw);
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
