package com.g42.platform.gms.customermerge.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bảng so sánh nhiều hồ sơ khách để nhân viên chọn giữ thông tin nào trước khi gộp. */
@Data
public class CustomerCompareDto {

    private List<Customer> customers = new ArrayList<>();

    /** Từng trường (hoặc cụm trường đi cùng nhau như địa chỉ) và giá trị ở mỗi hồ sơ. */
    private List<Field> fields = new ArrayList<>();

    /** Biển số mà sau khi gộp sẽ có nhiều dòng xe — sẽ được gộp thành một. */
    private List<PlateGroup> duplicatePlates = new ArrayList<>();

    @Data
    public static class Customer {
        private Integer customerId;
        private String customerCode;
        private String fullName;
        private String createdAt;
        private String primaryPhone;
        private List<Phone> otherPhones = new ArrayList<>();
        private String accountStatus;
        private boolean hasPin;
        private String lastLoginAt;
        private int totalPoints;
        private int lifetimePoints;
        private List<DuplicateGroupDto.VehicleRow> vehicles = new ArrayList<>();
        /** Nhãn tiếng Việt → số bản ghi sẽ chuyển sang hồ sơ giữ lại (chỉ các bảng có dữ liệu). */
        private Map<String, Integer> relatedCounts = new LinkedHashMap<>();
    }

    @Data
    public static class Phone {
        private String phone;
        private String note;
    }

    @Data
    public static class Field {
        private String key;
        private String label;
        /** customerId (dạng chuỗi) → giá trị hiển thị; null/rỗng = hồ sơ đó để trống. */
        private Map<String, String> values = new LinkedHashMap<>();
        private boolean differs;
        /** Hồ sơ mặc định được lấy giá trị nếu nhân viên không chọn. */
        private Integer defaultSourceCustomerId;
    }

    @Data
    public static class PlateGroup {
        private String plateKey;
        private List<DuplicateGroupDto.VehicleRow> vehicles = new ArrayList<>();
    }
}
