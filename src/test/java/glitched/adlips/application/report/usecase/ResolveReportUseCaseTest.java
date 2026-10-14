package glitched.adlips.application.report.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.report.port.out.ReportContentModerationPort;
import glitched.adlips.application.report.port.out.ReportResolutionPort;
import glitched.adlips.application.user.admin.port.out.UserBanRepositoryPort;
import glitched.adlips.domain.admin.UserBan;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResolveReportUseCaseTest {

    ReportResolutionPort port;
    ReportContentModerationPort contentPort;
    UserBanRepositoryPort userBanRepository;
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
        contentPort = mock(ReportContentModerationPort.class);
        userBanRepository = mock(UserBanRepositoryPort.class);
        useCase = new ResolveReportUseCase(port, contentPort, userBanRepository, userRepository, clock, transactionRunner);

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

        ReportResolutionResult result = useCase.execute(101L, 2L, "NONE", "조치 없이 처리합니다.");

        assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(result.actionType()).isEqualTo(ModerationActionType.NONE);
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

        assertThatThrownBy(() -> useCase.execute(101L, 3L, "NONE", "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.FORBIDDEN);

        verify(port, never()).findByIdForUpdate(any());
    }

    @Test
    void 존재하지_않는_사용자면_FORBIDDEN_예외가_발생한다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(101L, 999L, "NONE", "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.FORBIDDEN);
    }

    @Test
    void reason이_공백이면_VALIDATION_ERROR_예외가_발생하고_포트를_호출하지_않는다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "NONE", "  "))
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

        assertThatThrownBy(() -> useCase.execute(999L, 2L, "NONE", "사유"))
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

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "NONE", "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.ALREADY_HANDLED);

        verify(port, never()).save(any());
    }

    private Report pendingReport(ReportTargetType targetType, Long targetId) {
        return new Report(reporter, targetType, targetId, "ABUSE", "설명");
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }

    @Test
    void DELETE_CONTENT는_신고_대상을_같은_트랜잭션에서_삭제한다() {
        List<String> calls = new ArrayList<>();
        doAnswer(invocation -> {
            calls.add("tx-begin");
            Object result = invocation.<Supplier<?>>getArgument(0).get();
            calls.add("tx-end");
            return result;
        }).when(transactionRunner).required(any());
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(pendingReport(ReportTargetType.POST, 50L)));
        doAnswer(invocation -> calls.add("delete")).when(contentPort)
                .deleteContent(ReportTargetType.POST, 50L, now());
        doAnswer(invocation -> calls.add("action")).when(port).saveModerationAction(any());

        useCase.execute(101L, 2L, "DELETE_CONTENT", "음란물");

        assertThat(calls).containsExactly("tx-begin", "delete", "action", "tx-end");
    }

    @Test
    void DELETE_CONTENT는_게시글_댓글과_숏폼_댓글에도_적용된다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(pendingReport(ReportTargetType.POST_COMMENT, 70L)));
        when(port.findByIdForUpdate(102L)).thenReturn(Optional.of(pendingReport(ReportTargetType.SHORT_COMMENT, 80L)));

        useCase.execute(101L, 2L, "DELETE_CONTENT", "욕설");
        useCase.execute(102L, 2L, "DELETE_CONTENT", "욕설");

        verify(contentPort).deleteContent(ReportTargetType.POST_COMMENT, 70L, now());
        verify(contentPort).deleteContent(ReportTargetType.SHORT_COMMENT, 80L, now());
    }

    @Test
    void 숏폼_삭제는_지원하지_않아_VALIDATION_ERROR가_발생한다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        Report report = pendingReport(ReportTargetType.SHORT, 12L);
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "DELETE_CONTENT", "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.VALIDATION_ERROR);
        verify(contentPort, never()).deleteContent(any(), any(), any());
        verify(port, never()).saveModerationAction(any());
    }

    @Test
    void HIDE_CONTENT는_지원하지_않아_VALIDATION_ERROR가_발생한다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "HIDE_CONTENT", "사유"))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.VALIDATION_ERROR);
        verify(port, never()).findByIdForUpdate(any());
    }

    @Test
    void BAN_USER는_작성자를_입력한_기간만큼_정지한다() {
        User owner = User.restore(5L, "owner@example.com", UserRole.USER, null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(5L)).thenReturn(Optional.of(owner));
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(pendingReport(ReportTargetType.POST, 50L)));
        when(contentPort.findOwnerId(ReportTargetType.POST, 50L)).thenReturn(Optional.of(5L));
        when(userBanRepository.findActiveByUserId(5L, now())).thenReturn(Optional.empty());

        useCase.execute(101L, 2L, "BAN_USER", "반복 도배", 7);

        ArgumentCaptor<UserBan> captor = ArgumentCaptor.forClass(UserBan.class);
        verify(userBanRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(owner);
        assertThat(captor.getValue().getAdmin()).isEqualTo(admin);
        assertThat(captor.getValue().getReason()).isEqualTo("반복 도배");
        assertThat(captor.getValue().getBannedUntil()).isEqualTo(now().plusDays(7));
        verify(contentPort, never()).deleteContent(any(), any(), any());
    }

    @Test
    void BAN_USER_기간이_없으면_영구_정지한다() {
        User owner = User.restore(5L, "owner@example.com", UserRole.USER, null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(5L)).thenReturn(Optional.of(owner));
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(pendingReport(ReportTargetType.SHORT, 12L)));
        when(contentPort.findOwnerId(ReportTargetType.SHORT, 12L)).thenReturn(Optional.of(5L));

        useCase.execute(101L, 2L, "BAN_USER", "불법 음원", null);

        ArgumentCaptor<UserBan> captor = ArgumentCaptor.forClass(UserBan.class);
        verify(userBanRepository).save(captor.capture());
        assertThat(captor.getValue().getBannedUntil()).isNull();
    }

    @Test
    void BAN_USER_기간이_1일_미만이면_VALIDATION_ERROR가_발생한다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "BAN_USER", "사유", 0))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.VALIDATION_ERROR);
        verify(port, never()).findByIdForUpdate(any());
    }

    @Test
    void 이미_정지된_작성자면_ALREADY_BANNED_USER가_발생하고_신고는_처리되지_않는다() {
        User owner = User.restore(5L, "owner@example.com", UserRole.USER, null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(5L)).thenReturn(Optional.of(owner));
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(pendingReport(ReportTargetType.POST, 50L)));
        when(contentPort.findOwnerId(ReportTargetType.POST, 50L)).thenReturn(Optional.of(5L));
        when(userBanRepository.findActiveByUserId(5L, now()))
                .thenReturn(Optional.of(UserBan.create(owner, admin, "기존", null)));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "BAN_USER", "사유", 7))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.ALREADY_BANNED_USER);
        verify(userBanRepository, never()).save(any());
        verify(port, never()).saveModerationAction(any());
    }

    @Test
    void 관리자_본인이나_다른_관리자는_정지할_수_없다() {
        User otherAdmin = User.restore(6L, "admin2@example.com", UserRole.ADMIN, null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(6L)).thenReturn(Optional.of(otherAdmin));
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(pendingReport(ReportTargetType.POST, 50L)));
        when(contentPort.findOwnerId(ReportTargetType.POST, 50L)).thenReturn(Optional.of(6L));

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "BAN_USER", "사유", 7))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.VALIDATION_ERROR);
        verify(userBanRepository, never()).save(any());
    }

    @Test
    void 대상_작성자를_찾을_수_없으면_TARGET_NOT_FOUND가_발생한다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(pendingReport(ReportTargetType.POST, 50L)));
        when(contentPort.findOwnerId(ReportTargetType.POST, 50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(101L, 2L, "BAN_USER", "사유", 7))
                .isInstanceOf(ReportResolutionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ReportResolutionErrorCode.TARGET_NOT_FOUND);
    }

    @Test
    void WARN_USER와_NONE은_기록만_남긴다() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(port.findByIdForUpdate(101L)).thenReturn(Optional.of(pendingReport(ReportTargetType.POST, 50L)));

        useCase.execute(101L, 2L, "WARN_USER", "경고");

        verify(port).saveModerationAction(any(ModerationAction.class));
        verify(contentPort, never()).deleteContent(any(), any(), any());
        verify(userBanRepository, never()).save(any());
        verify(contentPort, never()).findOwnerId(any(), any());
    }
}
