package com.g42.platform.gms.estimation.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * CỐ Ý không dùng @AllArgsConstructor: các câu truy vấn JPQL gọi hàm dựng theo đúng số
 * tham số, nên thêm một trường mới sẽ lặng lẽ đổi chữ ký và làm hỏng truy vấn lúc khởi
 * động. Hai hàm dựng dưới đây khai báo tường minh để điều đó không xảy ra nữa.
 */
@Getter
@Setter
@NoArgsConstructor
public class InactiveCustomerDto {
    private Integer customerId;
    private String fullName;
    private String phone;
    private Integer serviceTicketId;
    private Integer vehicleId;
    private String licensePlate;
    private LocalDateTime lastVisitDate;

    /**
     * Lần cuối đến xưởng lấy từ đâu: SYSTEM là phiếu dịch vụ trong phần mềm, LEGACY là
     * lượt nhập từ sổ Excel cũ. Lễ tân cần biết để không đi tìm một phiếu không tồn tại.
     */
    private String source;

    /** Số ngày kể từ lần cuối khách đến xưởng, tính sẵn cho giao diện. */
    private Integer daysSinceLastVisit;

    /** Dùng bởi các truy vấn trên phiếu dịch vụ. Giữ nguyên chữ ký 7 tham số. */
    public InactiveCustomerDto(Integer customerId, String fullName, String phone,
                               Integer serviceTicketId, Integer vehicleId,
                               String licensePlate, LocalDateTime lastVisitDate) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.phone = phone;
        this.serviceTicketId = serviceTicketId;
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.lastVisitDate = lastVisitDate;
    }

    /** Dùng bởi truy vấn lượt cũ — dữ liệu legacy không có mã phiếu dịch vụ. */
    public InactiveCustomerDto(Integer customerId, String fullName, String phone,
                               Integer vehicleId, String licensePlate, LocalDateTime lastVisitDate) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.phone = phone;
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.lastVisitDate = lastVisitDate;
    }
}
