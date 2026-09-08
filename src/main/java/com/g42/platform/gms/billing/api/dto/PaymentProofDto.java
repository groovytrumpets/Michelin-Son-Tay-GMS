package com.g42.platform.gms.billing.api.dto;

import com.g42.platform.gms.billing.infrastructure.entity.PaymentProofJpa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Một chứng từ thanh toán (ảnh/video) trả về cho FE. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProofDto {
    private Integer paymentProofId;
    private Integer serviceTicketId;
    private Integer billId;
    private String mediaType;
    private String url;
    private String publicId;
    private String fileName;
    private Long fileSize;
    private String note;
    private Integer uploadedBy;
    private LocalDateTime createdAt;

    public static PaymentProofDto from(PaymentProofJpa e) {
        return PaymentProofDto.builder()
                .paymentProofId(e.getPaymentProofId())
                .serviceTicketId(e.getServiceTicketId())
                .billId(e.getBillId())
                .mediaType(e.getMediaType())
                .url(e.getUrl())
                .publicId(e.getPublicId())
                .fileName(e.getFileName())
                .fileSize(e.getFileSize())
                .note(e.getNote())
                .uploadedBy(e.getUploadedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
