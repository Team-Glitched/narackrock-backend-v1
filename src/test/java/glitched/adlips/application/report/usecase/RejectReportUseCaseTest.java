package glitched.adlips.application.report.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.report.port.out.ReportResolutionPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.report.ModerationAction;
import glitched.adlips.domain.report.ModerationActionType;
import glitched.adlips.domain.report.Report;
import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RejectReportUseCaseTest {

    ReportResolutionPort port;
    UserRepositoryPort userRepository;
    TransactionRunner transactionRunner;
    RejectReportUseCase useCase;
    User admin;
    User reporter;

    @BeforeEach
    void setUp() {
        port = mock(ReportResolutionPort.class);
        userRepository = mock(UserRepositoryPort.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new RejectReportUseCase(
                port,
                userRepository,
                Clock.fixed(Instant.parse("2026-06-24T05:45:00Z"), ZoneOffset.UTC),
                transactionRunner);
        admin = User.restore(2L, "admin@example.com", UserRole.ADMIN, null);
        reporter = User.create("reporter@example.com").withId(1L);
    }

    @Test
    void 정상_반려하면_REJECTED_상태와_REJECT_REPORT_조치가_저장된다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(
                new Report(reporter, ReportTargetType.SHORT, 12L, "OTHER", "설명")));

        ReportResolutionResult result = useCase.execute(101L, 2L, "정책 위반으로 보기 어렵습니다.");

        assertThat(result.status()).isEqualTo(ReportStatus.REJECTED);
        assertThat(result.actionType()).isEqualTo(ModerationActionType.REJECT_REPORT);
        verify(port).save(any(Report.class));
        verify(port).saveModerationAction(any(ModerationAction.class));
    }

    @Test
    void 관리자가_아니면_FORBIDDEN이고_신고를_조회하지_않는다() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(
                User.restore(3L, "user@example.com", UserRole.USER, null)));

        assertThatThrownBy(() -> useCase.execute(101L, 3L, "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.FORBIDDEN);

        verify(port, never()).findByIdForUpdate(any());
    }

    @Test
    void 반려_사유가_공백이면_VALIDATION_ERROR이고_신고를_조회하지_않는다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "  "))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.VALIDATION_ERROR);

        verify(port, never()).findByIdForUpdate(any());
    }
}
