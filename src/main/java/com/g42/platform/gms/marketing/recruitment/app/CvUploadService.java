package com.g42.platform.gms.marketing.recruitment.app;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Nhận file CV ứng viên đính kèm.
 *
 * <p>Không dùng chung {@code ImageUploadService} vì CV thường là PDF/DOCX —
 * dịch vụ kia chỉ nhận ảnh và còn nén lại bằng thư viện ảnh.
 *
 * <p>Đây là điểm tải lên CÔNG KHAI (ứng viên chưa đăng nhập), nên phần kiểm tra
 * ở đây chặt hơn hẳn các nơi khác: danh sách đuôi file đóng, kiểm tra cả phần
 * mở rộng lẫn kiểu MIME, và giới hạn dung lượng cứng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CvUploadService {

    private static final String FOLDER = "garage/recruitment/cv";

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private static final List<String> ALLOWED_EXTENSIONS =
            List.of("pdf", "doc", "docx", "jpg", "jpeg", "png");

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png");

    private final Cloudinary cloudinary;

    /** @return đường dẫn công khai của file vừa tải lên. */
    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn file CV");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File tối đa 10MB, vui lòng nén lại hoặc gửi bản PDF");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Chỉ nhận file PDF, DOC, DOCX hoặc ảnh JPG/PNG");
        }

        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ");
        }

        Map<String, Object> options = new HashMap<>();
        options.put("folder", FOLDER);
        // "raw" giữ nguyên file: Cloudinary không cố diễn giải PDF/DOCX thành ảnh.
        options.put("resource_type", "raw");
        // Với resource_type raw, đuôi file phải nằm trong public_id thì đường dẫn
        // trả về mới kết thúc bằng .pdf/.docx — nhờ đó trình duyệt của người
        // tuyển mở đúng ứng dụng khi bấm tải. Tên đặt ngẫu nhiên, không lấy theo
        // tên file gốc: tên file do người lạ gửi lên, và thường chứa họ tên thật.
        options.put("public_id", "cv-" + UUID.randomUUID() + "." + extension);
        options.put("use_filename", false);
        options.put("unique_filename", false);

        @SuppressWarnings("rawtypes")
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
        String url = (String) uploadResult.get("secure_url");
        log.info("Tuyển dụng: đã nhận một file CV ({} KB)", file.getSize() / 1024);
        return url;
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
