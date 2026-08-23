package com.g42.platform.gms.customerimport.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Toàn bộ nội dung một lần nhập, dùng chung cho cả bước kiểm tra thử và bước ghi. */
@Getter
@Setter
@NoArgsConstructor
public class CustomerImportRequest {

    /** Cách xử lý khi biển số trong file đã thuộc về một khách khác trong hệ thống. */
    public enum PlateConflictPolicy {
        /** Giữ chủ xe hiện tại, lượt vẫn ghi cho khách trong file và vẫn gắn xe. Mặc định. */
        KEEP_OWNER,
        /** Chuyển xe sang cho khách trong file. */
        TRANSFER,
        /** Không gắn xe vào lượt này, chỉ ghi lượt cho khách. */
        SKIP_VEHICLE
    }

    private String fileName;
    private String sheetName;
    private String note;

    private PlateConflictPolicy plateConflictPolicy = PlateConflictPolicy.KEEP_OWNER;

    private List<ImportRowDto> rows = new ArrayList<>();
}
