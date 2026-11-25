package com.g42.platform.gms.systemlog.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/** Mô tả lấy nguyên từ bảng system_log trên production (dump 25/11/2026). */
class SystemLogDescriberTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Nhật ký tự động cũ: đường dẫn API thành câu tiếng Việt, giữ lại đường dẫn gốc")
    void rawApiDescription() {
        SystemLogDescriber.Described d = SystemLogDescriber.describe(
                "PUT /api/warehouse/catalog-item/update/1306 (itemId=1306)");
        assertThat(d.summary()).isEqualTo("Sửa mặt hàng (danh mục & tồn kho)");
        assertThat(d.api()).isEqualTo("PUT /api/warehouse/catalog-item/update/1306");
        assertThat(d.changes()).isEmpty();
        assertThat(d.noise()).isFalse();
    }

    @Test
    @DisplayName("Đổi trạng thái phiếu / báo giá hiện tên trạng thái tiếng Việt")
    void statusLabels() {
        assertThat(SystemLogDescriber.describe(
                "PUT /api/service-ticket/manage/315/PAID (serviceTicketId=315, status=PAID)").summary())
                .isEqualTo("Chuyển phiếu sang «Đã thanh toán»");
        assertThat(SystemLogDescriber.describe(
                "PUT /api/service-ticket/estimate/435/CANCELLED (estimateId=435, status=CANCELLED)").summary())
                .isEqualTo("Chuyển báo giá sang «Đã huỷ»");
        assertThat(SystemLogDescriber.describe("PUT /api/service-ticket/estimate/461 (estimateId=461)").summary())
                .isEqualTo("Sửa báo giá");
        assertThat(SystemLogDescriber.describe("POST /api/service-ticket/estimate/").summary())
                .isEqualTo("Tạo báo giá");
        assertThat(SystemLogDescriber.describe(
                "POST /api/service-ticket/estimate/446/stock-allocation (estimateId=446)").summary())
                .isEqualTo("Giữ hàng theo báo giá");
    }

    @Test
    @DisplayName("Thao tác vặt được đánh dấu để ẩn/làm mờ")
    void noise() {
        assertThat(SystemLogDescriber.describe("PUT /api/staff-notification/233/isReaded (notificationId=233)").noise()).isTrue();
        assertThat(SystemLogDescriber.describe(
                "PUT /api/staff/table-prefs/gms_warehouse_table_columns_v2 (tableKey=gms_warehouse_table_columns_v2)").noise()).isTrue();
        assertThat(SystemLogDescriber.describe(
                "POST /api/public/posts/gara-o-to-son-tay-nao-uy-tin/view (slug=gara-o-to-son-tay-nao-uy-tin)").summary())
                .isEqualTo("Khách xem bài viết");
        assertThat(SystemLogDescriber.describe("POST /api/admin/posts/upload-image").summary()).isEqualTo("Tải ảnh lên");
        assertThat(SystemLogDescriber.describe("POST /api/warehouse/stock-entries/2104/confirm (id=2104)").noise()).isFalse();
    }

    @Test
    @DisplayName("Nhật ký ghi tay đã là tiếng Việt thì giữ nguyên; đường dẫn lạ vẫn đọc được")
    void manualAndUnknown() {
        assertThat(SystemLogDescriber.describe("Nhập khách hàng và lịch sử dịch vụ từ Excel").summary())
                .isEqualTo("Nhập khách hàng và lịch sử dịch vụ từ Excel");
        assertThat(SystemLogDescriber.describe("POST /api/some-new/feature/9").summary())
                .isEqualTo("Tạo / gửi · /api/some-new/feature/9");
    }

    @Test
    @DisplayName("Ghi chú của service (AuditNote) tách lại được thành tiêu đề, thay đổi trước → sau và đường dẫn")
    void auditNoteRoundTrip() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        AuditNote.headline("Sửa mặt hàng «BUGI  HYUNDAI/KIA ACCENT 1885510060» (SKU 1885510060)");
        AuditNote.change("Tồn kho CS1 TÂN PHÚC: 40 → 0");
        AuditNote.change("Lô NK-20261104-6 (CS1 TÂN PHÚC) còn: 40 → 0");

        String stored = AuditNote.consume("PUT /api/warehouse/catalog-item/update/1306", null);
        SystemLogDescriber.Described d = SystemLogDescriber.describe(stored);

        assertThat(d.summary()).isEqualTo("Sửa mặt hàng «BUGI  HYUNDAI/KIA ACCENT 1885510060» (SKU 1885510060)");
        assertThat(d.changes()).containsExactly(
                "Tồn kho CS1 TÂN PHÚC: 40 → 0",
                "Lô NK-20261104-6 (CS1 TÂN PHÚC) còn: 40 → 0");
        assertThat(d.api()).isEqualTo("PUT /api/warehouse/catalog-item/update/1306");
        // Đọc xong thì xoá, request sau không dính ghi chú cũ
        assertThat(AuditNote.consume("PUT /x", null)).isNull();
    }

    @Test
    @DisplayName("Ghi chú quá dài bị cắt gọn trong giới hạn cột description (1000 ký tự)")
    void auditNoteTruncates() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        AuditNote.headline("Sửa mặt hàng «X»");
        for (int i = 0; i < 100; i++) {
            AuditNote.change("Lô NK-20261104-" + i + " (CS1 TÂN PHÚC) còn: 40 → 0");
        }
        String stored = AuditNote.consume("PUT /api/warehouse/catalog-item/update/1", null);
        assertThat(stored.length()).isLessThanOrEqualTo(1000);
        assertThat(stored).contains("thay đổi khác").endsWith("API: PUT /api/warehouse/catalog-item/update/1");
    }

    @Test
    @DisplayName("skip(): lần lưu không đổi gì thì không ghi nhật ký")
    void skip() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        assertThat(AuditNote.consumeSkip()).isFalse();
        AuditNote.skip();
        assertThat(AuditNote.consumeSkip()).isTrue();
        assertThat(AuditNote.consumeSkip()).isFalse();
    }
}
