package com.g42.platform.gms.customercare.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO của màn gọi chăm sóc khách. Khớp contract FE: src/services/customerCareService.js.
 *
 * Gom chung một file vì toàn là hộp dữ liệu nhỏ đi cùng nhau, tách mỗi lớp một file chỉ
 * làm khó đọc contract.
 */
public final class CareDtos {

    private CareDtos() {
    }

    /** Một dòng của danh sách — tương ứng một dòng của file Excel cũ, đã tính sẵn mọi cột. */
    @Data
    public static class CareCustomerRow {
        private Integer customerId;
        private String customerCode;
        private String fullName;

        /** Số để gọi: số chính, không có thì số phụ đầu tiên. */
        private String phone;
        /** Mọi số còn lại của khách (số phụ ở customer_phone). */
        private List<String> extraPhones = new ArrayList<>();
        /** false = số sai định dạng (thiếu/thừa chữ số) — file cũ có "097151923". */
        private boolean phoneValid;
        private String email;

        private List<VehicleBrief> vehicles = new ArrayList<>();

        /** Cột "Tổng" của file cũ: hoá đơn đã thu trong phần mềm + tiền chép tay ở sổ cũ. */
        private BigDecimal totalSpend = BigDecimal.ZERO;
        private BigDecimal systemSpend = BigDecimal.ZERO;
        private BigDecimal legacySpend = BigDecimal.ZERO;

        private int visitCount;
        private int systemVisitCount;
        private int legacyVisitCount;
        private LocalDateTime lastVisitAt;
        /** SYSTEM | LEGACY */
        private String lastVisitSource;
        private Integer daysSinceLastVisit;

        /** Mã CareStatus + nhãn tiếng Việt. */
        private String status;
        private String statusLabel;
        /** Đến lượt phải gọi (chưa gọi, hoặc đã tới ngày hẹn gọi lại). */
        private boolean due;
        private LocalDate followUpDate;
        /** Khách đã đến xưởng SAU cuộc gọi gần nhất — cuộc gọi có hiệu quả, bắt đầu vòng chăm sóc mới. */
        private boolean returnedAfterCall;

        /** Số cuộc gọi, tính cả kết quả gọi ghi ở sổ cũ. */
        private int callCount;
        private CallEntry lastCall;

        private boolean doNotContact;

        /** Cột "Lặp sđt" của file cũ: hồ sơ khác dùng chung SĐT hoặc biển số. */
        private List<DuplicateHint> duplicates = new ArrayList<>();

        private String careNote;
        private String preferredTime;
    }

    @Data
    public static class VehicleBrief {
        private Integer vehicleId;
        private String licensePlate;
        private String brand;
        private String model;
    }

    @Data
    public static class DuplicateHint {
        private Integer customerId;
        private String fullName;
        /** VD "Cùng SĐT 0912345678", "Cùng biển số 30K-86694". */
        private String reason;
    }

    /** Một cuộc gọi — trong phần mềm, hoặc kết quả gọi chép ở sổ cũ. */
    @Data
    public static class CallEntry {
        /** null với dòng từ sổ cũ. */
        private Integer careCallId;
        /** SYSTEM | LEGACY */
        private String source;
        private Integer legacyVisitId;
        private LocalDateTime calledAt;
        /** true = sổ cũ không ghi ngày gọi, calledAt là ngày của lượt dịch vụ đó. */
        private boolean dateApproximate;
        private String phone;
        private Integer staffId;
        private String staffName;
        private boolean reached;
        private String outcome;
        private String outcomeLabel;
        private String note;
        private LocalDate followUpDate;
    }

    @Data
    public static class CareListResponse {
        private List<CareCustomerRow> content = new ArrayList<>();
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        /** Số khách theo từng tab (DUE, ALL và từng CareStatus), tính trên bộ lọc hiện tại trừ tab. */
        private Map<String, Integer> counts = new LinkedHashMap<>();
        /** Tổng chi tiêu của mọi khách khớp bộ lọc (không chỉ trang đang xem). */
        private BigDecimal filteredSpend = BigDecimal.ZERO;
        private TodayStats today = new TodayStats();
    }

    @Data
    public static class TodayStats {
        private int calls;
        private int reached;
        private int booked;
    }

    @Data
    public static class CareCustomerDetail {
        private CareCustomerRow customer;
        /** Mới nhất trước; gồm cả cuộc gọi trong phần mềm và kết quả gọi ở sổ cũ. */
        private List<CallEntry> calls = new ArrayList<>();
    }

    @Data
    public static class OutcomeOption {
        private String code;
        private String label;
        private boolean reached;
        private Integer defaultFollowUpDays;
        private boolean stopsContact;
    }

    @Data
    public static class CallRequest {
        private String phone;
        private String outcome;
        private String note;
        private LocalDate followUpDate;
        /** true + followUpDate trống = lấy số ngày gợi ý của kết quả (nút gọi nhanh "Không bắt máy"). */
        private Boolean autoFollowUp;
        /** Trống = bây giờ. Điền khi ghi bù một cuộc gọi đã gọi trước đó. */
        private LocalDateTime calledAt;
    }

    @Data
    public static class CareProfileRequest {
        private String careNote;
        private String preferredTime;
    }

    @Data
    public static class DoNotContactRequest {
        private Boolean doNotContact;
    }
}
