package com.g42.platform.gms.marketing.news;

import com.g42.platform.gms.marketing.news.app.PostContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bộ lọc HTML của bài viết là ranh giới tin cậy: nó vừa phải giữ được định dạng
 * do trình soạn thảo sinh ra (căn lề, cỡ chữ, giãn dòng), vừa phải chặn được
 * mã độc. Hai yêu cầu này kéo ngược nhau nên cần khoá lại bằng test.
 */
class PostContentServiceTest {

    private final PostContentService service = new PostContentService();

    // ------------------------------------------------- định dạng phải giữ lại

    @Test
    @DisplayName("Giữ căn lề đoạn văn và chuẩn hoá khoảng trắng của khai báo CSS")
    void keepsTextAlign() {
        // Chrome sinh "text-align: center", Firefox có thể sinh "text-align:center";
        // sau khi lọc, cả hai đều phải ra cùng một dạng.
        assertThat(service.sanitize("<p style=\"text-align:center\">Xin chào</p>"))
                .contains("text-align: center");
        assertThat(service.sanitize("<p style=\"text-align:   center\">Xin chào</p>"))
                .contains("text-align: center");
    }

    @Test
    @DisplayName("Giữ cỡ chữ do trình soạn thảo đặt")
    void keepsFontSize() {
        String result = service.sanitize("<p><span style=\"font-size:24px\">To</span></p>");
        assertThat(result).contains("font-size: 24px");
    }

    @Test
    @DisplayName("Giữ giãn dòng và khoảng cách trên dưới")
    void keepsLineHeightAndSpacing() {
        String result = service.sanitize(
                "<p style=\"line-height:1.8; margin-top:24px; margin-bottom:40px\">Đoạn văn</p>");
        assertThat(result).contains("line-height: 1.8");
        assertThat(result).contains("margin-top: 24px");
        assertThat(result).contains("margin-bottom: 40px");
    }

    @Test
    @DisplayName("Giữ ảnh kèm căn lề của khối chứa nó")
    void keepsAlignedImage() {
        String result = service.sanitize(
                "<p style=\"text-align:right\"><img src=\"https://res.cloudinary.com/a.png\" alt=\"Lốp xe\"></p>");
        assertThat(result).contains("text-align: right");
        assertThat(result).contains("https://res.cloudinary.com/a.png");
        assertThat(result).contains("alt=\"Lốp xe\"");
    }

    @Test
    @DisplayName("Giữ cỡ ảnh do biên tập đặt trong menu chuột phải")
    void keepsImageWidth() {
        String result = service.sanitize(
                "<p><img src=\"https://res.cloudinary.com/a.png\" style=\"width:50%; height:auto\"></p>");
        assertThat(result).contains("width: 50%");
        assertThat(result).contains("height: auto");
    }

    @Test
    @DisplayName("Ảnh xem tạm chưa tải xong không được lưu xuống")
    void dropsBlobImages() {
        // Trình soạn thảo đã gỡ ảnh blob trước khi gửi, nhưng nếu lọt tới đây thì
        // đường dẫn blob: cũng không nằm trong danh sách giao thức cho phép.
        String result = service.sanitize("<p><img src=\"blob:http://localhost:5173/abc-123\"></p>");
        assertThat(result).doesNotContain("blob:");
    }

    // ------------------------------------------------------ mã độc phải chặn

    @Test
    @DisplayName("Loại bỏ thẻ script")
    void removesScript() {
        String result = service.sanitize("<p>Chào</p><script>alert(1)</script>");
        assertThat(result).doesNotContain("script");
        assertThat(result).contains("Chào");
    }

    @Test
    @DisplayName("Loại bỏ thuộc tính sự kiện và liên kết javascript")
    void removesEventHandlersAndJavascriptLinks() {
        String result = service.sanitize(
                "<p onclick=\"steal()\">Bấm</p><a href=\"javascript:alert(1)\">Nhấn</a>");
        assertThat(result).doesNotContain("onclick");
        assertThat(result).doesNotContain("javascript:");
    }

    @Test
    @DisplayName("Loại bỏ khai báo CSS nằm ngoài danh sách cho phép")
    void dropsDisallowedCssProperties() {
        String result = service.sanitize(
                "<p style=\"position:fixed; top:0; z-index:99999; text-align:center\">Phủ màn hình</p>");
        assertThat(result).doesNotContain("position");
        assertThat(result).doesNotContain("z-index");
        // Khai báo hợp lệ đứng cùng dòng vẫn phải sống sót.
        assertThat(result).contains("text-align: center");
    }

    @Test
    @DisplayName("Loại bỏ giá trị CSS chứa url() hoặc javascript:")
    void dropsDangerousCssValues() {
        String result = service.sanitize(
                "<p style=\"background-color:url(javascript:alert(1)); color:#ff0000\">Chữ đỏ</p>");
        assertThat(result).doesNotContain("javascript");
        assertThat(result).doesNotContain("url(");
        assertThat(result).contains("color: #ff0000");
    }

    @Test
    @DisplayName("Chỉ cho nhúng video từ nguồn đã duyệt")
    void keepsOnlyTrustedIframes() {
        String youtube = service.sanitize(
                "<iframe src=\"https://www.youtube.com/embed/abc123\" title=\"Video\"></iframe>");
        assertThat(youtube).contains("youtube.com/embed/abc123");

        String other = service.sanitize("<iframe src=\"https://ke-xau.example/khung\"></iframe>");
        assertThat(other).doesNotContain("ke-xau.example");
    }

    // ------------------------------------------------------- thông tin dẫn xuất

    @Test
    @DisplayName("Tóm tắt tự động bóc hết thẻ và cắt theo ranh giới từ")
    void buildsExcerptFromPlainText() {
        String excerpt = service.buildExcerpt(
                "<p style=\"text-align:center\">Hướng dẫn <b>thay lốp</b> đúng cách cho xe gầm cao</p>", 20);
        assertThat(excerpt).doesNotContain("<");
        assertThat(excerpt).startsWith("Hướng dẫn thay lốp");
        assertThat(excerpt).endsWith("…");
    }

    @Test
    @DisplayName("Thời gian đọc luôn ít nhất một phút")
    void readingTimeIsAtLeastOneMinute() {
        assertThat(service.estimateReadingMinutes("<p>Ngắn</p>")).isEqualTo(1);
        assertThat(service.estimateReadingMinutes("")).isEqualTo(1);
    }

    @Test
    @DisplayName("Lấy được ảnh đầu tiên để làm ảnh đại diện")
    void findsFirstImage() {
        String html = "<p>Mở bài</p><p><img src=\"https://res.cloudinary.com/first.png\"></p>"
                + "<p><img src=\"https://res.cloudinary.com/second.png\"></p>";
        assertThat(service.firstImageUrl(html)).isEqualTo("https://res.cloudinary.com/first.png");
        assertThat(service.firstImageUrl("<p>Không có ảnh</p>")).isNull();
    }
}
