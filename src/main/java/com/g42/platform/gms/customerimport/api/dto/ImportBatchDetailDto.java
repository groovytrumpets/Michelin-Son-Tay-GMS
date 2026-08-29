package com.g42.platform.gms.customerimport.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Toàn cảnh một lô đã nhập: lô đó đưa vào hệ thống những khách nào, mỗi khách kèm xe
 * và các lượt dịch vụ đã ghi.
 *
 * Lô còn hiệu lực thì dựng từ dữ liệu thật trong hệ thống, nên bấm vào là mở đúng hồ
 * sơ khách. Lô đã hoàn tác không còn lượt nào, khi đó dựng lại từ nội dung file đã lưu
 * để vẫn xem lại được lô đó từng có gì — cờ {@code fromStoredRows} đánh dấu trường hợp
 * này và các khách khi đó không có mã để mở hồ sơ.
 */
@Getter
@Setter
@NoArgsConstructor
public class ImportBatchDetailDto {

    private Integer batchId;
    private String fileName;
    private String sheetName;
    private String note;
    private String status;
    private String plateConflictPolicy;
    private LocalDateTime importedAt;
    private Integer importedBy;
    private String importedByName;
    private Integer replacedByBatchId;

    /** Thống kê ghi lại lúc chạy lô. */
    private int customersCreated;
    private int customersMerged;
    private int vehiclesCreated;
    private int visitsCreated;
    private int visitItemsCreated;
    private int skippedRows;

    /** Số lượt của lô còn nằm trong hệ thống ngay lúc này. */
    private int visitsRemaining;

    /** true = dựng từ nội dung file đã lưu vì lô không còn lượt nào trong hệ thống. */
    private boolean fromStoredRows;

    private List<Customer> customers = new ArrayList<>();

    /** Dòng có trong file nhưng không tạo ra lượt nào: trùng, thiếu ngày hoặc bị loại. */
    private List<MissingRow> rowsWithoutVisit = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Customer {
        private Integer customerId;
        private String fullName;
        private String phone;
        private String email;
        private String customerCode;
        /** true = hồ sơ do chính lô này tạo ra, hoàn tác lô sẽ xoá đi. */
        private boolean createdByBatch;
        private int visitCount;
        private BigDecimal totalAmount;
        private LocalDateTime lastVisitedAt;
        private List<Vehicle> vehicles = new ArrayList<>();
        private List<Visit> visits = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Vehicle {
        private Integer vehicleId;
        private String licensePlate;
        private String brand;
        private String model;
        private Integer manufactureYear;
        private boolean createdByBatch;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Visit {
        private Integer legacyVisitId;
        private Integer sourceRowNo;
        private String legacyTicketCode;
        private LocalDateTime visitedAt;
        private LocalDateTime deliveredAt;
        private String licensePlate;
        private Integer odometer;
        private String customerNote;
        private String servicesText;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private boolean amountMismatch;
        private List<Item> items = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {
        private String category;
        private String itemName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MissingRow {
        private Integer sourceRowNo;
        private String fullName;
        private String phone;
        private String licensePlate;
        private String visitedDate;
    }
}
