package com.g42.platform.gms.bugreport.dto;

import com.g42.platform.gms.bugreport.enums.BugReportCategory;
import com.g42.platform.gms.bugreport.enums.BugReportSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BugReportCreateRequest {

    @NotBlank(message = "Vui lòng nhập tiêu đề lỗi")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @NotBlank(message = "Vui lòng mô tả chi tiết lỗi")
    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
    private String description;

    @NotNull(message = "Vui lòng chọn loại phản hồi")
    private BugReportCategory category;

    @NotNull(message = "Vui lòng chọn mức độ ảnh hưởng")
    private BugReportSeverity severity;

    @Size(max = 40)
    private String module;

    @Size(max = 100)
    private String reporterContact;

    // Ngữ cảnh kỹ thuật do frontend tự thu thập
    @Size(max = 500)
    private String pageUrl;

    @Size(max = 40)
    private String screenSize;

    @Size(max = 40)
    private String appVersion;

    /** Tối đa 5 ảnh, đã upload lên Cloudinary trước khi gọi API này. */
    private List<String> attachmentUrls;
}
