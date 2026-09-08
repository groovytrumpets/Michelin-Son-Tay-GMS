package com.g42.platform.gms.billing.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Yêu cầu lưu chứng từ thanh toán. FE đã upload file lên Cloudinary trước rồi
 * gửi danh sách URL sang đây (thêm nhiều file trong một lần).
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentProofCreateDto {

    /** Hoá đơn đang thao tác, có thể null nếu chưa tạo bill. */
    private Integer billId;

    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {
        /** IMAGE hoặc VIDEO. */
        private String mediaType;
        private String url;
        private String publicId;
        private String fileName;
        private Long fileSize;
        private String note;
    }
}
