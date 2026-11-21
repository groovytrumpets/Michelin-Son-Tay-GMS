package com.g42.platform.gms.report.application.service.export;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ghi file .xlsx theo kiểu luồng (SXSSF) cho báo cáo tổng hợp.
 *
 * <p>Xuất "từ trước đến nay" có thể ra hàng chục nghìn dòng hạng mục — XSSF thường giữ cả file
 * trong RAM, SXSSF chỉ giữ một cửa sổ dòng và đẩy phần còn lại ra file tạm. Đổi lại không
 * autoSize cột được (cần đọc lại mọi dòng), nên mỗi cột tự khai báo độ rộng.
 */
public final class XlsxWriter implements AutoCloseable {

    /** Excel chặn ô dài quá 32.767 ký tự — vượt là cả file hỏng. */
    private static final int MAX_CELL_TEXT = 32_000;
    private static final int ROW_WINDOW = 200;

    public enum Fmt { TEXT, WRAP, INT, QTY, MONEY, PERCENT, DATE, DATETIME }

    /** Một cột: tiêu đề, độ rộng (số ký tự) và định dạng. */
    public record Col(String header, int width, Fmt fmt) {
        public static Col text(String header, int width) { return new Col(header, width, Fmt.TEXT); }
        public static Col wrap(String header, int width) { return new Col(header, width, Fmt.WRAP); }
        public static Col integer(String header) { return new Col(header, 10, Fmt.INT); }
        public static Col qty(String header) { return new Col(header, 12, Fmt.QTY); }
        public static Col money(String header) { return new Col(header, 16, Fmt.MONEY); }
        public static Col percent(String header) { return new Col(header, 11, Fmt.PERCENT); }
        public static Col date(String header) { return new Col(header, 12, Fmt.DATE); }
        public static Col dateTime(String header) { return new Col(header, 17, Fmt.DATETIME); }
    }

    private final SXSSFWorkbook workbook;
    private final Map<Fmt, CellStyle> styles = new EnumMap<>(Fmt.class);
    private final CellStyle headerStyle;
    private final CellStyle titleStyle;
    private final CellStyle sectionStyle;
    private final CellStyle labelStyle;
    private final CellStyle noteStyle;

    public XlsxWriter() {
        workbook = new SXSSFWorkbook(ROW_WINDOW);
        workbook.setCompressTempFiles(true);
        DataFormat df = workbook.createDataFormat();

        for (Fmt fmt : Fmt.values()) {
            CellStyle style = workbook.createCellStyle();
            style.setVerticalAlignment(VerticalAlignment.TOP);
            switch (fmt) {
                case WRAP -> style.setWrapText(true);
                case INT -> style.setDataFormat(df.getFormat("#,##0"));
                case QTY -> style.setDataFormat(df.getFormat("#,##0.###"));
                case MONEY -> style.setDataFormat(df.getFormat("#,##0"));
                case PERCENT -> style.setDataFormat(df.getFormat("0.0%"));
                case DATE -> style.setDataFormat(df.getFormat("dd/mm/yyyy"));
                case DATETIME -> style.setDataFormat(df.getFormat("dd/mm/yyyy hh:mm"));
                default -> { }
            }
            styles.put(fmt, style);
        }

        Font bold = workbook.createFont();
        bold.setBold(true);
        Font white = workbook.createFont();
        white.setBold(true);
        white.setColor(IndexedColors.WHITE.getIndex());
        Font big = workbook.createFont();
        big.setBold(true);
        big.setFontHeightInPoints((short) 15);
        Font italic = workbook.createFont();
        italic.setItalic(true);
        italic.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        headerStyle = workbook.createCellStyle();
        headerStyle.setFont(white);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setWrapText(true);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        titleStyle = workbook.createCellStyle();
        titleStyle.setFont(big);

        sectionStyle = workbook.createCellStyle();
        sectionStyle.setFont(bold);
        sectionStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        labelStyle = workbook.createCellStyle();
        labelStyle.setFont(bold);

        noteStyle = workbook.createCellStyle();
        noteStyle.setFont(italic);
        noteStyle.setWrapText(true);
    }

    /** Tạo sheet bảng: dòng tiêu đề cố định, bật lọc. Gọi {@link Table#row} cho từng dòng. */
    public Table table(String name, List<Col> cols) {
        return new Table(workbook.createSheet(safeName(name)), cols);
    }

    /** Sheet dạng "nhãn — giá trị" (Tổng quan). */
    public KeyValue keyValue(String name) {
        return new KeyValue(workbook.createSheet(safeName(name)));
    }

    public byte[] toBytes() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public void close() throws IOException {
        // POI 5.4+: close() cũng xoá file tạm của SXSSF.
        workbook.close();
    }

    public final class Table {
        private final SXSSFSheet sheet;
        private final List<Col> cols;
        private int next = 1;

        private Table(SXSSFSheet sheet, List<Col> cols) {
            this.sheet = sheet;
            this.cols = cols;
            Row header = sheet.createRow(0);
            header.setHeightInPoints(32);
            for (int i = 0; i < cols.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols.get(i).header());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, Math.min(255, Math.max(6, cols.get(i).width())) * 256);
            }
            sheet.createFreezePane(0, 1);
        }

        public void row(Object... values) {
            Row row = sheet.createRow(next++);
            for (int i = 0; i < values.length && i < cols.size(); i++) {
                write(row, i, values[i], cols.get(i).fmt());
            }
        }

        public int rowCount() {
            return next - 1;
        }

        /** Bật lọc trên dòng tiêu đề — gọi sau khi ghi xong (cần biết dòng cuối). */
        public void finish() {
            if (cols.isEmpty()) return;
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, next - 1), 0, cols.size() - 1));
        }
    }

    public final class KeyValue {
        private final SXSSFSheet sheet;
        private int next = 0;

        private KeyValue(SXSSFSheet sheet) {
            this.sheet = sheet;
            sheet.setColumnWidth(0, 42 * 256);
            sheet.setColumnWidth(1, 30 * 256);
            sheet.setColumnWidth(2, 70 * 256);
        }

        public void title(String text) {
            Cell cell = sheet.createRow(next++).createCell(0);
            cell.setCellValue(text);
            cell.setCellStyle(titleStyle);
        }

        public void section(String text) {
            next++;
            Row row = sheet.createRow(next++);
            for (int i = 0; i < 3; i++) {
                Cell cell = row.createCell(i);
                if (i == 0) cell.setCellValue(text);
                cell.setCellStyle(sectionStyle);
            }
        }

        public void pair(String label, Object value, Fmt fmt) {
            pair(label, value, fmt, null);
        }

        public void pair(String label, Object value, Fmt fmt, String note) {
            Row row = sheet.createRow(next++);
            Cell cell = row.createCell(0);
            cell.setCellValue(label);
            cell.setCellStyle(labelStyle);
            write(row, 1, value, fmt);
            if (note != null && !note.isBlank()) {
                Cell n = row.createCell(2);
                n.setCellValue(note);
                n.setCellStyle(noteStyle);
            }
        }

        public void note(String text) {
            Cell cell = sheet.createRow(next++).createCell(0);
            cell.setCellValue(text);
            cell.setCellStyle(noteStyle);
            sheet.addMergedRegion(new CellRangeAddress(next - 1, next - 1, 0, 2));
        }
    }

    private void write(Row row, int col, Object value, Fmt fmt) {
        if (value == null) return;
        Cell cell = row.createCell(col);
        CellStyle style = styles.get(fmt);
        if (value instanceof BigDecimal b) {
            cell.setCellValue(b.doubleValue());
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof LocalDateTime dt) {
            cell.setCellValue(dt);
            if (fmt != Fmt.DATE && fmt != Fmt.DATETIME) style = styles.get(Fmt.DATETIME);
        } else if (value instanceof LocalDate d) {
            cell.setCellValue(d);
            if (fmt != Fmt.DATE && fmt != Fmt.DATETIME) style = styles.get(Fmt.DATE);
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b ? "Có" : "Không");
            style = styles.get(Fmt.TEXT);
        } else {
            String text = value.toString();
            if (text.isEmpty()) return;
            cell.setCellValue(text.length() > MAX_CELL_TEXT ? text.substring(0, MAX_CELL_TEXT) + "…" : text);
            if (fmt != Fmt.WRAP) style = styles.get(Fmt.TEXT);
        }
        cell.setCellStyle(style);
    }

    /** Tên sheet: tối đa 31 ký tự, không chứa : \ / ? * [ ]. */
    private static String safeName(String name) {
        String cleaned = name.replaceAll("[:\\\\/?*\\[\\]]", "-");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }
}
