package com.g42.platform.gms.customermerge.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Một nhóm hồ sơ khách nghi là cùng một người, gom theo biển số xe.
 *
 * reason:
 * - DUPLICATE_VEHICLE: có từ 2 dòng xe cùng biển số (viết lệch nhau) thuộc các khách khác nhau.
 *   Nhóm này CHẶN ràng buộc duy nhất của biển số, không bỏ qua được — phải gộp hồ sơ, hoặc
 *   xác nhận khác người rồi chỉ gộp xe về một chủ.
 * - SHARED_HISTORY: chỉ một dòng xe, nhưng có lượt sổ cũ của khách khác gắn vào xe đó (nhập sổ
 *   cũ theo SĐT: người 2 số bị tách 2 hồ sơ, hồ sơ sau "mượn" xe của hồ sơ trước).
 */
@Data
public class DuplicateGroupDto {

    public static final String REASON_DUPLICATE_VEHICLE = "DUPLICATE_VEHICLE";
    public static final String REASON_SHARED_HISTORY = "SHARED_HISTORY";

    private String groupKey;
    private String plateKey;
    private String reason;
    private boolean blocksUniquePlate;
    private List<String> plates = new ArrayList<>();
    private List<VehicleRow> vehicles = new ArrayList<>();
    private List<CustomerSummary> customers = new ArrayList<>();

    @Data
    public static class VehicleRow {
        private Integer vehicleId;
        private String licensePlate;
        private String brand;
        private String model;
        private Integer manufactureYear;
        private Integer ownerCustomerId;
    }

    @Data
    public static class CustomerSummary {
        private Integer customerId;
        private String customerCode;
        private String fullName;
        private String phone;
        private List<String> otherPhones = new ArrayList<>();
        private String email;
        private String address;
        private String createdAt;
        private String accountStatus;
        private int serviceTicketCount;
        private int bookingCount;
        private int legacyVisitCount;
        /** Khách này dính vào nhóm vì là chủ xe (true) hay chỉ vì có lượt sổ cũ gắn vào xe (false). */
        private boolean vehicleOwner;
    }
}
