package glitched.adlips.application.report.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.report.port.out.ReportResolutionPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.report.ModerationAction;
import glitched.adlips.domain.report.ModerationActionType;
import glitched.adlips.domain.report.Report;
import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.LocalDateTime;

public class ResolveReportUseCase {

    private final ReportResolutionPort port;
    private final UserRepositoryPort userRepository;
    private final Clock clock;
    private final TransactionRunner transactionRunner;

    public ResolveReportUseCase(
            ReportResolutionPort port,
            UserRepositoryPort userRepository,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        this.port = port;
        this.userRepository = userRepository;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
    }

    public ReportResolutionResult execute(Long reportId, Long adminUserId, String actionTypeValue, String reason) {
        User admin = requireAdmin(adminUserId);

        if (reason == null || reason.isBlank()) {
            throw new ReportResolutionApplicationException(
                    ReportResolutionErrorCode.VALIDATION_ERROR, "차단 원인을 입력해 주세요.");
        }
        ModerationActionType actionType = parseActionType(actionTypeValue);

        return transactionRunner.required(() -> resolve(reportId, admin, actionType, reason.trim()));
    }

    private ReportResolutionResult resolve(Long reportId, User admin, ModerationActionType actionType, String reason) {
        Report report = port.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ReportResolutionApplicationException(
                        ReportResolutionErrorCode.REPORT_NOT_FOUND, "존재하지 않는 신고입니다."));

        ReportStatus newStatus = actionType == ModerationActionType.REJECT_REPORT
                ? ReportStatus.REJECTED : ReportStatus.RESOLVED;
        try {
            report.resolve(admin, newStatus, LocalDateTime.now(clock));
        } catch (IllegalStateException e) {
            throw new ReportResolutionApplicationException(
                    ReportResolutionErrorCode.ALREADY_HANDLED, "이미 처리된 신고입니다.");
        }
        port.save(report);
        port.saveModerationAction(new ModerationAction(
                report, admin, report.getTargetType(), report.getTargetId(), actionType, reason));

        return new ReportResolutionResult(
                report.getId(), report.getStatus(), actionType,
                report.getHandledAt(), report.getTargetType(), report.getTargetId());
    }

    private User requireAdmin(Long adminUserId) {
        return userRepository.findById(adminUserId)
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new ReportResolutionApplicationException(
                        ReportResolutionErrorCode.FORBIDDEN, "관리자만 처리할 수 있습니다."));
    }

    private ModerationActionType parseActionType(String value) {
        try {
            return ModerationActionType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ReportResolutionApplicationException(
                    ReportResolutionErrorCode.VALIDATION_ERROR, "올바르지 않은 처리 유형입니다.");
        }
    }
}
