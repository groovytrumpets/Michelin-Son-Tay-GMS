package com.g42.platform.gms.customermerge.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Một nhóm hồ sơ khách nghi là cùng một người, gom theo biển số xe.
 *
 * Từ changeset 039, trùng biển số KHÔNG còn là lỗi: vợ chồng / gia đình / công ty dùng chung
 * một xe thì mỗi người vẫn có hồ sơ riêng. Nên đây chỉ là GỢI Ý để nhân viên tự quyết, không
 * có nhóm nào bắt buộc phải gộp.
 *
 * reason:
 * - SAME_OWNER_DUPLICATE: cùng MỘT khách có nhiều dòng xe cùng biển số — đây là nhập trùng
 *   thật, nên gộp xe (không mất gì). Đáng chú ý nhất.
 * - DUPLICATE_VEHICLE: nhiều dòng xe cùng biển số thuộc các khách KHÁC NHAU. Có thể là một
 *   người bị tạo hai hồ sơ (gộp), cũng có thể là xe dùng chung (bỏ qua).
 * - SHARED_HISTORY: chỉ một dòng xe, nhưng có lượt sổ cũ của khách khác gắn vào xe đó (nhập sổ
 *   cũ theo SĐT: người 2 số bị tách 2 hồ sơ, hồ sơ sau "mượn" xe của hồ sơ trước).
 */
@Data
public class DuplicateGroupDto {

    public static final String REASON_DUPLICATE_VEHICLE = "DUPLICATE_VEHICLE";
    public static final String REASON_SAME_OWNER_DUPLICATE = "SAME_OWNER_DUPLICATE";
    public static final String REASON_SHARED_HISTORY = "SHARED_HISTORY";

    private String groupKey;
    private String plateKey;
    private String reason;

    /**
     * Nhóm gần như chắc chắn là lỗi dữ liệu (cùng một khách có 2 dòng xe cùng biển số) — xếp
     * lên đầu. Các nhóm còn lại chỉ là gợi ý, bỏ qua được.
     */
    private boolean needsAttention;
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
