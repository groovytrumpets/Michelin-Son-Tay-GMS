package com.g42.platform.gms.customerimport.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Nội dung một lô đã nhập, trả về để người dùng mở lại lên bảng, sửa rồi nhập lại.
 *
 * Lô ghi từ bản sau khi có cột rows_json thì trả đúng nguyên văn những dòng đã gửi.
 * Lô cũ hơn không có bản gốc nên phải dựng lại từ dữ liệu đã ghi — khi đó cờ
 * {@code reconstructed} bật lên và những dòng từng bị bỏ qua sẽ không có mặt.
 */
@Getter
@Setter
@NoArgsConstructor
public class ImportBatchRowsDto {

    private Integer batchId;
    private String fileName;
    private String sheetName;
    private String note;
    private String status;
    private String plateConflictPolicy;

    /** true = dựng lại từ dữ liệu đã ghi, không phải nguyên văn lần nhập trước. */
    private boolean reconstructed;

    private List<ImportRowDto> rows = new ArrayList<>();
}
