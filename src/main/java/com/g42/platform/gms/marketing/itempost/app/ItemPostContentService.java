package com.g42.platform.gms.marketing.itempost.app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Xử lý nội dung HTML của bài viết phụ tùng: làm sạch trước khi lưu, và rút ra
 * các thông tin phái sinh (tóm tắt, thời gian đọc, ảnh đầu tiên).
 *
 * <p>Trình soạn thảo phía trình duyệt không phải là ranh giới tin cậy — bất kỳ
 * ai có tài khoản biên tập cũng có thể gửi thẳng HTML tới API, nên việc lọc bắt
 * buộc phải làm ở server.
 */
@Service
public class ItemPostContentService {

    /** Số từ đọc được trong một phút, dùng để ước lượng thời gian đọc. */
    private static final int WORDS_PER_MINUTE = 200;

    private final Safelist safelist = buildSafelist();

    /** Thẻ được phép mang thuộc tính style — căn lề, cỡ chữ, giãn dòng đều nằm ở đây. */
    private static final String[] STYLEABLE_TAGS = {
            "p", "div", "span", "h1", "h2", "h3", "h4", "li", "ul", "ol",
            "blockquote", "figure", "figcaption", "img", "a",
            // Bảng: đường kẻ, nền hàng tiêu đề và độ rộng cột đều nằm ở style nội tuyến.
            "table", "thead", "tbody", "tfoot", "tr", "td", "th", "caption"
    };

    /**
     * Thuộc tính CSS được giữ lại. Danh sách đóng: chỉ những gì phục vụ trình bày
     * bài viết. Không có định vị (position, z-index) hay nền dạng ảnh — đó là
     * những thứ có thể dùng để che phủ giao diện trang.
     */
    private static final Set<String> ALLOWED_CSS_PROPERTIES = Set.of(
            "text-align", "font-size", "line-height", "font-weight", "font-style",
            "text-decoration", "color", "background-color",
            "margin", "margin-top", "margin-bottom", "margin-left", "margin-right",
            "padding", "padding-top", "padding-bottom", "padding-left", "padding-right",
            "width", "height", "max-width", "min-width", "display", "float", "vertical-align",
            "border", "border-radius", "text-indent", "letter-spacing",
            // Kẻ bảng: viền từng cạnh và cách gộp đường kẻ giữa các ô.
            "border-collapse", "border-color", "border-style", "border-width",
            "border-top", "border-right", "border-bottom", "border-left", "table-layout"
    );

    /** Giá trị CSS chứa những mẫu này thì loại bỏ cả khai báo. */
    private static final Pattern DANGEROUS_CSS_VALUE =
            Pattern.compile("(?i)(url\\s*\\(|expression\\s*\\(|javascript:|@import|behavior\\s*:|<)");

    private static Safelist buildSafelist() {
        Safelist safelist = Safelist.relaxed()
                // Cho phép nhúng video YouTube — nội dung hướng dẫn sửa xe hay dùng.
                .addTags("figure", "figcaption", "hr", "span", "iframe")
                .addAttributes("iframe", "src", "width", "height", "allow", "allowfullscreen", "frameborder", "title")
                .addProtocols("iframe", "src", "https")
                .addAttributes("h2", "id")
                .addAttributes("h3", "id")
                .addAttributes("img", "src", "alt", "title", "width", "height", "loading")
                .addAttributes("a", "href", "title", "target", "rel")
                .addProtocols("img", "src", "https", "http", "data")
                .preserveRelativeLinks(false);

        for (String tag : STYLEABLE_TAGS) {
            safelist.addAttributes(tag, "style");
        }
        return safelist;
    }

    /** Lọc HTML biên tập gửi lên: bỏ thẻ script, thuộc tính sự kiện và giao thức javascript. */
    public String sanitize(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) return "";
        String cleaned = Jsoup.clean(rawHtml, "", safelist);

        // Chỉ giữ iframe trỏ về YouTube; nguồn nhúng khác là rủi ro không cần thiết.
        cleaned = cleaned.replaceAll(
                "(?is)<iframe(?![^>]*\\bsrc=\"https://(www\\.)?(youtube\\.com|youtube-nocookie\\.com|player\\.vimeo\\.com)/)[^>]*>.*?</iframe>",
                "");

        return filterInlineStyles(cleaned);
    }

    /**
     * Lọc nội dung thuộc tính {@code style}.
     *
     * <p>Jsoup chỉ xét thẻ và tên thuộc tính, nó không đọc bên trong chuỗi CSS.
     * Vì trình soạn thảo cần style để lưu căn lề/cỡ chữ/giãn dòng, phần giá trị
     * phải được duyệt riêng ở đây — nếu không, một khai báo kiểu
     * {@code position:fixed} hay {@code background:url(...)} vẫn lọt qua và có
     * thể phủ lên giao diện trang tin.
     */
    private String filterInlineStyles(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        for (Element element : document.select("[style]")) {
            String filtered = keepAllowedDeclarations(element.attr("style"));
            if (filtered.isEmpty()) element.removeAttr("style");
            else element.attr("style", filtered);
        }
        return document.body().html();
    }

    /**
     * Giữ lại các khai báo hợp lệ và ghi lại theo một khuôn duy nhất
     * {@code thuộc-tính: giá-trị}. Mỗi trình duyệt sinh ra khoảng trắng một kiểu,
     * chuẩn hoá ở đây để nội dung lưu xuống không phụ thuộc vào nơi soạn thảo.
     */
    private String keepAllowedDeclarations(String styleValue) {
        return Arrays.stream(styleValue.split(";"))
                .map(String::trim)
                .filter(declaration -> !declaration.isEmpty())
                .map(declaration -> {
                    int colon = declaration.indexOf(':');
                    if (colon <= 0) return null;
                    String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                    String value = declaration.substring(colon + 1).trim();
                    boolean allowed = ALLOWED_CSS_PROPERTIES.contains(property)
                            && !value.isEmpty()
                            && !DANGEROUS_CSS_VALUE.matcher(value).find();
                    return allowed ? property + ": " + value : null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining("; "));
    }

    /** Bóc chữ trần khỏi HTML, dùng cho tóm tắt và cho thẻ meta description. */
    public String toPlainText(String html) {
        if (html == null || html.isBlank()) return "";
        return Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
    }

    /** Tóm tắt tự động khi biên tập bỏ trống ô tóm tắt. */
    public String buildExcerpt(String html, int maxLength) {
        String text = toPlainText(html);
        if (text.length() <= maxLength) return text;
        String cut = text.substring(0, maxLength);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > maxLength / 2) cut = cut.substring(0, lastSpace);
        return cut.trim() + "…";
    }

    /** Ước lượng số phút đọc, hiện dưới tiêu đề bài. */
    public int estimateReadingMinutes(String html) {
        String text = toPlainText(html);
        if (text.isEmpty()) return 1;
        int words = text.split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / (double) WORDS_PER_MINUTE));
    }

    /** Ảnh đầu tiên trong bài — dùng làm ảnh chia sẻ khi biên tập chưa đặt ảnh bìa. */
    public String firstImageUrl(String html) {
        if (html == null || html.isBlank()) return null;
        var img = Jsoup.parse(html).selectFirst("img[src]");
        return img == null ? null : img.attr("src");
    }
}
