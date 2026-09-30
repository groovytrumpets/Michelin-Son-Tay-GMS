package com.g42.platform.gms.customercare.domain;

import com.g42.platform.gms.customerimport.application.service.ImportNormalizer;

import java.util.List;

/**
 * Đoán mã kết quả từ ô "Note" chép tay của sổ cũ (legacy_visit.call_note).
 *
 * Chỉ để hiển thị và suy ra trạng thái cho khách chưa có cuộc gọi nào trong phần mềm —
 * không ghi ngược vào đâu cả. Đoán sai thì nhân viên gọi lần tới là trạng thái đúng lại.
 *
 * Thứ tự kiểm tra có chủ ý: "Đang ở xa, khi nào về được thì thay" phải ra AWAY trước khi
 * gặp "khi nao"; "Khi nào qua thì báo sau" phải ra WILL_VISIT trước khi gặp "bao sau".
 */
public final class LegacyCallNoteClassifier {

    private LegacyCallNoteClassifier() {
    }

    private record Rule(CareCallOutcome outcome, List<String> markers) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(CareCallOutcome.BLOCKED, List.of("chan so")),
            new Rule(CareCallOutcome.WRONG_NUMBER, List.of("so chet", "sai so", "nham so", "so khong dung")),
            new Rule(CareCallOutcome.UNREACHABLE, List.of("thue bao", "tat may", "khong lien lac", "ngoai vung")),
            new Rule(CareCallOutcome.NO_ANSWER, List.of("khong bat may", "k bat may", "ko bat may",
                    "khong nghe", "k nghe", "ko nghe", "khong nhac may")),
            new Rule(CareCallOutcome.BUSY, List.of("may ban", "dang ban")),
            new Rule(CareCallOutcome.REQUEST_STOP, List.of("khong muon", "dung goi", "khong goi nua")),
            new Rule(CareCallOutcome.NOT_DUE, List.of("chua den ky", "chua den han", "chua toi ky",
                    "chua toi han", "chua can")),
            new Rule(CareCallOutcome.AWAY, List.of("o xa", "di xa", "ve que", "cong tac")),
            // Phải đứng trước WILL_VISIT, vì "không quay lại" cũng chứa "quay lai"
            new Rule(CareCallOutcome.DECLINED, List.of("khong quay lai", "khong qua nua")),
            new Rule(CareCallOutcome.WILL_VISIT, List.of("se qua", "khi nao qua", "vai hom", "se den", "qua sau",
                    "quay lai")),
            new Rule(CareCallOutcome.THINKING, List.of("suy nghi", "bao lai", "bao sau")),
            new Rule(CareCallOutcome.DECLINED, List.of("khong qua", "khong co nhu cau", "khong can", "khong lam")),
            new Rule(CareCallOutcome.BOOKED, List.of("da hen", "da dat lich", "hen lich", "hen ngay"))
    );

    public static CareCallOutcome classify(String note, boolean callSuccess) {
        String key = ImportNormalizer.stripDiacritics(note);
        if (key != null) {
            for (Rule rule : RULES) {
                for (String marker : rule.markers()) {
                    if (key.contains(marker)) return rule.outcome();
                }
            }
        }
        return callSuccess ? CareCallOutcome.OTHER : CareCallOutcome.NO_ANSWER;
    }
}
