package com.g42.platform.gms.common.service;

import com.g42.platform.gms.auth.entity.StaffAuth;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.service.SensitiveActionVerificationService.GrantInfo;
import com.g42.platform.gms.common.service.SensitiveActionVerificationService.Purpose;
import com.g42.platform.gms.common.service.SensitiveActionVerificationService.VerificationFailedException;
import com.g42.platform.gms.notification.infrastructure.EmailNotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * Lớp xác minh email trước khi xuất báo cáo / tải bản sao lưu: phiên phải tách riêng theo việc,
 * sai mã quá số lần phải huỷ mã, token sai / của người khác phải bị từ chối bằng 428.
 */
@ExtendWith(MockitoExtension.class)
class SensitiveActionVerificationServiceTest {

    private static final Pattern CODE_IN_SUBJECT = Pattern.compile(": (\\d{6})$");

    @Mock
    private EmailNotificationSender emailSender;

    private SensitiveActionVerificationService service;
    private StaffPrincipal manager;

    @BeforeEach
    void setUp() {
        service = new SensitiveActionVerificationService(emailSender);
        manager = staff(7, "quanly@example.com");
        lenient().when(emailSender.isConfigured()).thenReturn(true);
    }

    @Test
    void grantForReportExportDoesNotUnlockBackupDownload() {
        GrantInfo grant = verifyWithEmailedCode(Purpose.REPORT_EXPORT, manager);

        assertThatCode(() -> service.requireGrant(Purpose.REPORT_EXPORT, manager, grant.exportToken()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.requireGrant(Purpose.DATA_BACKUP, manager, grant.exportToken()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.PRECONDITION_REQUIRED))
                .hasMessageContaining("tải bản sao lưu");
        assertThat(service.status(Purpose.DATA_BACKUP, manager).verifiedUntil()).isNull();
        assertThat(service.status(Purpose.REPORT_EXPORT, manager).verifiedUntil()).isNotNull();
    }

    @Test
    void backupGrantWorksOnlyForTheAccountThatVerified() {
        GrantInfo grant = verifyWithEmailedCode(Purpose.DATA_BACKUP, manager);
        StaffPrincipal other = staff(8, "khac@example.com");

        assertThatCode(() -> service.requireGrant(Purpose.DATA_BACKUP, manager, grant.exportToken()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.requireGrant(Purpose.DATA_BACKUP, other, grant.exportToken()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.requireGrant(Purpose.DATA_BACKUP, manager, "token-bia"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.requireGrant(Purpose.DATA_BACKUP, manager, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void emailChangedAfterVerifyingInvalidatesGrant() {
        GrantInfo grant = verifyWithEmailedCode(Purpose.DATA_BACKUP, manager);
        manager.getStaffAuth().setEmail("moi@example.com");

        assertThatThrownBy(() -> service.requireGrant(Purpose.DATA_BACKUP, manager, grant.exportToken()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void revokeEndsOnlyThatPurpose() {
        GrantInfo backup = verifyWithEmailedCode(Purpose.DATA_BACKUP, manager);
        GrantInfo report = verifyWithEmailedCode(Purpose.REPORT_EXPORT, manager);

        service.revoke(Purpose.DATA_BACKUP, manager);

        assertThatThrownBy(() -> service.requireGrant(Purpose.DATA_BACKUP, manager, backup.exportToken()))
                .isInstanceOf(ResponseStatusException.class);
        assertThatCode(() -> service.requireGrant(Purpose.REPORT_EXPORT, manager, report.exportToken()))
                .doesNotThrowAnyException();
    }

    @Test
    void wrongCodeFiveTimesCancelsTheCode() {
        String code = sendAndCaptureCode(Purpose.DATA_BACKUP, manager);
        String wrong = code.equals("123456") ? "654321" : "123456";

        for (int i = 1; i < SensitiveActionVerificationService.MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.verify(Purpose.DATA_BACKUP, manager, wrong))
                    .isInstanceOfSatisfying(VerificationFailedException.class, e -> assertThat(e.isLocked()).isFalse());
        }
        assertThatThrownBy(() -> service.verify(Purpose.DATA_BACKUP, manager, wrong))
                .isInstanceOfSatisfying(VerificationFailedException.class, e -> assertThat(e.isLocked()).isTrue());
        // Mã đúng cũng không dùng được nữa sau khi đã bị huỷ.
        assertThatThrownBy(() -> service.verify(Purpose.DATA_BACKUP, manager, code))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("hết hạn hoặc chưa được gửi");
    }

    @Test
    void codeIsSingleUse() {
        String code = sendAndCaptureCode(Purpose.DATA_BACKUP, manager);
        service.verify(Purpose.DATA_BACKUP, manager, code);

        assertThatThrownBy(() -> service.verify(Purpose.DATA_BACKUP, manager, code))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void codeSentForReportCannotVerifyBackup() {
        String code = sendAndCaptureCode(Purpose.REPORT_EXPORT, manager);

        assertThatThrownBy(() -> service.verify(Purpose.DATA_BACKUP, manager, code))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("hết hạn hoặc chưa được gửi");
    }

    @Test
    void resendWithinCooldownIsRejectedWith429() {
        sendAndCaptureCode(Purpose.DATA_BACKUP, manager);

        assertThatThrownBy(() -> service.sendCode(Purpose.DATA_BACKUP, manager, "10.0.0.1"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void accountWithoutEmailCannotRequestCode() {
        StaffPrincipal noEmail = staff(9, null);

        assertThatThrownBy(() -> service.sendCode(Purpose.DATA_BACKUP, noEmail, "10.0.0.1"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThat(service.status(Purpose.DATA_BACKUP, noEmail).hasEmail()).isFalse();
    }

    @Test
    void backupEmailNamesTheBackupNotTheReport() {
        sendAndCaptureCode(Purpose.DATA_BACKUP, manager);

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).sendHtmlOrThrow(anyList(), isNull(), subject.capture(), body.capture());
        assertThat(subject.getValue()).startsWith("Mã xác minh tải bản sao lưu dữ liệu: ");
        assertThat(body.getValue()).contains("bản sao lưu toàn bộ cơ sở dữ liệu").doesNotContain("báo cáo tổng hợp");
    }

    // ------------------------------------------------------------------------------------------

    private GrantInfo verifyWithEmailedCode(Purpose purpose, StaffPrincipal principal) {
        String code = sendAndCaptureCode(purpose, principal);
        GrantInfo grant = service.verify(purpose, principal, code);
        assertThat(grant.exportToken()).isNotBlank();
        return grant;
    }

    /** Mã chỉ đi qua email — lấy lại từ tiêu đề thư mà service gửi cho EmailNotificationSender. */
    private String sendAndCaptureCode(Purpose purpose, StaffPrincipal principal) {
        service.sendCode(purpose, principal, "10.0.0.1");
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).sendHtmlOrThrow(anyList(), isNull(), subject.capture(), anyString());
        Matcher m = CODE_IN_SUBJECT.matcher(subject.getValue());
        assertThat(m.find()).as("tiêu đề thư phải kết thúc bằng mã 6 số").isTrue();
        return m.group(1);
    }

    private static StaffPrincipal staff(int authId, String email) {
        StaffAuth auth = new StaffAuth();
        auth.setStaffAuthId(authId);
        auth.setEmail(email);
        return new StaffPrincipal(auth);
    }
}
