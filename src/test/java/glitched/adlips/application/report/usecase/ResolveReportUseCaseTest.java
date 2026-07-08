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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResolveReportUseCaseTest {

    ReportResolutionPort port;
    UserRepositoryPort userRepository;
    Clock clock;
    TransactionRunner transactionRunner;
    ResolveReportUseCase useCase;

    User admin;
    User reporter;

    @BeforeEach
    void setUp() {
        port = mock(ReportResolutionPort.class);
        userRepository = mock(UserRepositoryPort.class);
        clock = Clock.fixed(Instant.parse("2026-07-08T05:40:00Z"), ZoneOffset.UTC);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new ResolveReportUseCase(port, userRepository, clock, transactionRunner);

        admin = User.restore(2L, "admin@example.com", UserRole.ADMIN, null);
        reporter = User.create("reporter@example.com").withId(1L);
    }

    private Report pendingReport() {
        return new Report(reporter, ReportTargetType.SHORT, 12L, "COPYRIGHT", "설명");
    }

    @Test
    void 정상_처리하면_RESOLVED_상태로_저장되고_ModerationAction이_저장된다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(pendingReport()));

        ReportResolutionResult result = useCase.execute(101L, 2L, "HIDE_CONTENT", "운영 정책 위반으로 숨김 처리합니다.");

        assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(result.actionType()).isEqualTo(ModerationActionType.HIDE_CONTENT);
        assertThat(result.targetType()).isEqualTo(ReportTargetType.SHORT);
        assertThat(result.targetId()).isEqualTo(12L);
        assertThat(result.handledAt()).isEqualTo(LocalDateTime.ofInstant(clock.instant(), clock.getZone()));
        verify(port).save(any(Report.class));
        verify(port).saveModerationAction(any(ModerationAction.class));
    }

    @Test
    void REJECT_REPORT_액션은_별도_반려_API를_사용해야_하므로_VALIDATION_ERROR가_발생한다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "REJECT_REPORT", "근거 부족으로 반려합니다."))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.VALIDATION_ERROR);

        verify(port, never()).findByIdForUpdate(any());
    }

    @Test
    void 관리자가_아니면_FORBIDDEN_예외가_발생하고_이후_단계는_호출되지_않는다() {
        User normalUser = User.restore(3L, "user@example.com", UserRole.USER, null);
        when(userRepository.findById(3L)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> useCase.execute(101L, 3L, "HIDE_CONTENT", "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.FORBIDDEN);

        verify(port, never()).findByIdForUpdate(any());
    }

    @Test
    void 존재하지_않는_사용자면_FORBIDDEN_예외가_발생한다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(101L, 999L, "HIDE_CONTENT", "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.FORBIDDEN);
    }

    @Test
    void reason이_공백이면_VALIDATION_ERROR_예외가_발생하고_포트를_호출하지_않는다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "HIDE_CONTENT", "  "))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.VALIDATION_ERROR);

        verify(port, never()).findByIdForUpdate(any());
    }

    @Test
    void actionType이_올바르지_않으면_VALIDATION_ERROR_예외가_발생한다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "INVALID_TYPE", "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.VALIDATION_ERROR);

        verify(port, never()).findByIdForUpdate(any());
    }

    @Test
    void 신고가_없으면_REPORT_NOT_FOUND_예외가_발생한다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(port.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(999L, 2L, "HIDE_CONTENT", "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.REPORT_NOT_FOUND);

        verify(port, never()).save(any());
    }

    @Test
    void 이미_처리된_신고면_ALREADY_HANDLED_예외가_발생한다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        Report alreadyHandled = pendingReport();
        alreadyHandled.resolve(admin, ReportStatus.RESOLVED, LocalDateTime.now());
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(alreadyHandled));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "HIDE_CONTENT", "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.ALREADY_HANDLED);

        verify(port, never()).save(any());
    }
}
