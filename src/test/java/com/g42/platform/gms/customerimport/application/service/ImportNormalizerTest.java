package com.g42.platform.gms.customerimport.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Các trường hợp ở đây lấy nguyên từ sổ Excel của xưởng, không phải ví dụ bịa ra.
 * Sổ có 141 phiếu dùng được thì đã có 6 số điện thoại sai dạng và 4 cặp biển số chỉ
 * lệch nhau ở dấu gạch, nên đây là những quy tắc phải đúng ngay từ lần nhập đầu.
 */
class ImportNormalizerTest {

    @Test
    @DisplayName("Số điện thoại: gỡ số 0 thừa, mã quốc gia và dấu phân cách")
    void normalizePhone_handlesRealWorldNoise() {
        assertThat(ImportNormalizer.normalizePhone("00983898555")).isEqualTo("0983898555");
        assertThat(ImportNormalizer.normalizePhone("84987654321")).isEqualTo("0987654321");
        assertThat(ImportNormalizer.normalizePhone("+84 987 654 321")).isEqualTo("0987654321");
        assertThat(ImportNormalizer.normalizePhone("0335.955.181")).isEqualTo("0335955181");
        assertThat(ImportNormalizer.normalizePhone("0987 938 777")).isEqualTo("0987938777");
        assertThat(ImportNormalizer.normalizePhone("   ")).isNull();
        assertThat(ImportNormalizer.normalizePhone(null)).isNull();
    }

    @Test
    @DisplayName("Số điện thoại thiếu hoặc thừa chữ số phải bị coi là không hợp lệ, không tự đoán")
    void isValidPhone_rejectsWrongLength() {
        // Sáu số này có thật trong sổ — người nhập phải tự sửa, hệ thống không được đoán
        assertThat(ImportNormalizer.isValidPhone(ImportNormalizer.normalizePhone("097151923"))).isFalse();
        assertThat(ImportNormalizer.isValidPhone(ImportNormalizer.normalizePhone("079536668"))).isFalse();
        assertThat(ImportNormalizer.isValidPhone(ImportNormalizer.normalizePhone("09785779707"))).isFalse();
        assertThat(ImportNormalizer.isValidPhone(ImportNormalizer.normalizePhone("08885779999"))).isFalse();
        assertThat(ImportNormalizer.isValidPhone(ImportNormalizer.normalizePhone("09790550684"))).isFalse();

        assertThat(ImportNormalizer.isValidPhone("0335955181")).isTrue();
    }

    @Test
    @DisplayName("PIN khởi tạo là 6 số cuối của số điện thoại")
    void defaultPin_takesLastSixDigits() {
        assertThat(ImportNormalizer.defaultPin("0335955181")).isEqualTo("955181");
        assertThat(ImportNormalizer.defaultPin("0987938777")).isEqualTo("938777");
        assertThat(ImportNormalizer.defaultPin("123")).isNull();
        assertThat(ImportNormalizer.defaultPin(null)).isNull();
    }

    @Test
    @DisplayName("Biển số chỉ khác nhau ở dấu gạch phải cho ra cùng một khoá")
    void normalizePlate_ignoresPunctuation() {
        assertThat(ImportNormalizer.normalizePlate("30K-86694")).isEqualTo("30K86694");
        assertThat(ImportNormalizer.normalizePlate("30k86694")).isEqualTo("30K86694");
        assertThat(ImportNormalizer.normalizePlate(" 29K 16748 ")).isEqualTo("29K16748");
        assertThat(ImportNormalizer.normalizePlate("88A-32666")).isEqualTo("88A32666");
        assertThat(ImportNormalizer.normalizePlate("")).isNull();
    }

    @Test
    @DisplayName("Nhận dạng biển Việt Nam để cảnh báo ô nhập nhầm")
    void looksLikePlate_flagsNonPlates() {
        assertThat(ImportNormalizer.looksLikePlate("30K86694")).isTrue();
        assertThat(ImportNormalizer.looksLikePlate("30F85857")).isTrue();
        assertThat(ImportNormalizer.looksLikePlate("LSM")).isFalse();
        assertThat(ImportNormalizer.looksLikePlate("460")).isFalse();
    }

    @Test
    @DisplayName("Gộp lỗi chính tả tên hãng, nhưng không tự suy từ tên dòng xe")
    void canonicalBrand_onlyMergesMisspellings() {
        assertThat(ImportNormalizer.canonicalBrand("Hydai")).isEqualTo("Hyundai");
        assertThat(ImportNormalizer.canonicalBrand("Hyndai")).isEqualTo("Hyundai");
        assertThat(ImportNormalizer.canonicalBrand("Nisan")).isEqualTo("Nissan");
        assertThat(ImportNormalizer.canonicalBrand("Misu")).isEqualTo("Mitsubishi");
        assertThat(ImportNormalizer.canonicalBrand("Chevle")).isEqualTo("Chevrolet");

        // Đây là tên DÒNG xe bị điền nhầm vào ô hãng. Đoán ra hãng thì dễ sai, nên trả
        // về null để lớp trên báo cảnh báo cho người nhập tự quyết.
        assertThat(ImportNormalizer.canonicalBrand("Vios")).isNull();
        assertThat(ImportNormalizer.canonicalBrand("Santafe")).isNull();
        assertThat(ImportNormalizer.canonicalBrand("LSM")).isNull();
    }

    @Test
    @DisplayName("Ghi chú kiểu số chết hay chặn số phải bật cờ ngừng liên hệ")
    void isDoNotContactNote_matchesRealNotes() {
        assertThat(ImportNormalizer.isDoNotContactNote("Số chết")).isTrue();
        assertThat(ImportNormalizer.isDoNotContactNote("Chặn số")).isTrue();
        assertThat(ImportNormalizer.isDoNotContactNote("chan so")).isTrue();

        // Những ghi chú này vẫn còn cơ hội gọi lại, không được tắt liên hệ
        assertThat(ImportNormalizer.isDoNotContactNote("Không nghe máy")).isFalse();
        assertThat(ImportNormalizer.isDoNotContactNote("Khách chưa có nhu cầu")).isFalse();
        assertThat(ImportNormalizer.isDoNotContactNote("Chưa đến kỳ kiểm tra")).isFalse();
        assertThat(ImportNormalizer.isDoNotContactNote(null)).isFalse();
    }

    @Test
    @DisplayName("Khoá chống trùng ổn định giữa bước kiểm tra thử và bước ghi")
    void dedupeKey_isStableAndDistinguishes() {
        LocalDate day = LocalDate.of(2025, 10, 22);
        String a = ImportNormalizer.dedupeKey("0335955181", day, "1", List.of("Thay 2 lốp Michelin 255/55R19"));
        String b = ImportNormalizer.dedupeKey("0335955181", day, "1", List.of("Thay 2 lốp Michelin 255/55R19"));
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSizeLessThanOrEqualTo(120);

        // Cùng khách cùng ngày nhưng nội dung khác nhau là hai lượt riêng — sổ cũ có trường hợp này
        String other = ImportNormalizer.dedupeKey("0335955181", day, "1", List.of("Rửa xe"));
        assertThat(other).isNotEqualTo(a);

        // Mã phiếu bỏ trống vẫn phải ra khoá dùng được, không được là null
        String noCode = ImportNormalizer.dedupeKey("0335955181", day, null, List.of("Rửa xe"));
        assertThat(noCode).isNotBlank();
    }

    @Test
    @DisplayName("Chỉ nhận ngày dạng yyyy-MM-dd; serial Excel do FE quy đổi trước")
    void parseDate_acceptsIsoOnly() {
        assertThat(ImportNormalizer.parseDate("2025-10-22")).isEqualTo(LocalDate.of(2025, 10, 22));
        assertThat(ImportNormalizer.parseDate("45664")).isNull();
        assertThat(ImportNormalizer.parseDate("22/10/2025")).isNull();
        assertThat(ImportNormalizer.parseDate("  ")).isNull();
    }

    @Test
    @DisplayName("Bỏ dấu tiếng Việt để so khớp mờ")
    void stripDiacritics_normalizesVietnamese() {
        assertThat(ImportNormalizer.stripDiacritics("Cân bằng động")).isEqualTo("can bang dong");
        assertThat(ImportNormalizer.stripDiacritics("Giảm giá")).isEqualTo("giam gia");
        assertThat(ImportNormalizer.stripDiacritics("Lọc gió điều hòa")).isEqualTo("loc gio dieu hoa");
    }
}
