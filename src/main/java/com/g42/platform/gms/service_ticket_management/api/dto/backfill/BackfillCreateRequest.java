package com.g42.platform.gms.service_ticket_management.api.dto.backfill;

import com.g42.platform.gms.billing.api.dto.PaymentProofCreateDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Yêu cầu nhập bù một phiếu của ngày trước (từ /create-booking hoặc /parts-sales).
 *
 * FE lưu báo giá DRAFT trước bằng bảng báo giá sẵn có, upload ảnh chứng từ lên
 * Cloudinary, rồi gửi toàn bộ sang đây trong một lần — backend tạo phiếu, giữ hàng
 * và lưu chứng từ trong cùng transaction nên không thể có phiếu nhập bù thiếu chứng từ.
 */
@Getter
@Setter
@NoArgsConstructor
public class BackfillCreateRequest {

    /** MISSED (nhập lại cả phiếu) hoặc SUPPLEMENT (bổ sung dòng thiếu cho phiếu đã có). */
    private String kind;

    /** SERVICE (/create-booking) hoặc PARTS_SALE (/parts-sales). Với SUPPLEMENT lấy theo phiếu gốc. */
    private String ticketType;

    /** Phiếu gốc — bắt buộc với SUPPLEMENT. */
    private Integer parentTicketId;

    /** Khách đã có trong hệ thống. Phiếu sửa xe có thể để trống và gửi phone/biển số thay. */
    private Integer customerId;
    private String phone;
    private String fullName;
    private String licensePlate;
    /** Xe của khách (phiếu sửa xe). */
    private Integer vehicleId;

    /**
     * Thông tin xe như ô "Loại xe" ở /create-booking — chỉ ghi vào vehicle khi field đó
     * đang trống (cùng quy ước BookingService.createDirectBookingByStaff).
     */
    private String vehicleBrand;
    private String vehicleModel;
    private Integer vehicleYear;

    /** Số km đọc trên đồng hồ lúc xe vào — ghi odometer_reading gắn với phiếu. */
    private Integer odometerKm;

    /** Giờ trả xe thực tế (phiếu sửa xe) — hiện ở "Dự kiến hoàn tất", duyệt xong thành ngày bàn giao. */
    private LocalDateTime completedAt;

    /** Báo giá DRAFT chứa các dòng hàng/dịch vụ đã làm. */
    private Integer estimateId;

    /** Ngày giờ khách thực tế tới/nhận hàng. */
    private LocalDateTime actualServiceAt;

    private String reason;
    /** CASH hoặc TRANSFER. */
    private String paymentMethod;
    private String note;

    /**
     * Người đã làm, phân công lúc duyệt. Phiếu sửa xe: cố vấn (tuỳ chọn) + KTV (bắt buộc;
     * nhập thiếu dòng mà bỏ trống thì lấy theo phiếu gốc). Phiếu bán hàng: advisorId là
     * người bán — bỏ trống thì gán người nhập; technicianId bị bỏ qua.
     */
    private Integer advisorId;
    private Integer technicianId;

    /** Ảnh/video chứng từ đã upload — bắt buộc ít nhất 1. */
    private List<PaymentProofCreateDto.Item> proofs;
}
