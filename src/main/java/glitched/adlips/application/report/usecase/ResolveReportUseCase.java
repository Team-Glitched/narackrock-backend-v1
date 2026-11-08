package glitched.adlips.application.report.usecase;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.report.port.out.ReportContentModerationPort;
import glitched.adlips.application.report.port.out.ReportResolutionPort;
import glitched.adlips.application.user.admin.port.out.UserBanRepositoryPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.admin.UserBan;
import glitched.adlips.domain.report.ModerationAction;
import glitched.adlips.domain.report.ModerationActionType;
import glitched.adlips.domain.report.Report;
import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.report.ReportTargetType;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.time.Clock;
import java.time.LocalDateTime;

public class ResolveReportUseCase {
    private final NotificationEvents notifications;

    private final ReportResolutionPort port;
    private final ReportContentModerationPort contentPort;
    private final UserBanRepositoryPort userBanRepository;
    private final UserRepositoryPort userRepository;
    private final Clock clock;
    private final TransactionRunner transactionRunner;

    public ResolveReportUseCase(
            ReportResolutionPort port,
            ReportContentModerationPort contentPort,
            UserBanRepositoryPort userBanRepository,
            UserRepositoryPort userRepository,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        this(port, contentPort, userBanRepository, userRepository, clock, transactionRunner, NotificationEvents.none());
    }

    public ResolveReportUseCase(ReportResolutionPort port,
            ReportContentModerationPort contentPort,
            UserBanRepositoryPort userBanRepository,
            UserRepositoryPort userRepository,
            Clock clock,
            TransactionRunner transactionRunner, NotificationEvents notifications) {
        this.port = port;
        this.contentPort = contentPort;
        this.userBanRepository = userBanRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
    
        this.notifications = notifications;
    }

    public ReportResolutionResult execute(Long reportId, Long adminUserId, String actionTypeValue, String reason) {
        return execute(reportId, adminUserId, actionTypeValue, reason, null);
    }

    /** banDurationDays는 BAN_USER에서만 쓰며 null이면 영구 정지다. */
    public ReportResolutionResult execute(
            Long reportId, Long adminUserId, String actionTypeValue, String reason, Integer banDurationDays
    ) {
        User admin = requireAdmin(adminUserId);

        if (reason == null || reason.isBlank()) {
            throw validationError("차단 원인을 입력해 주세요.");
        }
        ModerationActionType actionType = parseActionType(actionTypeValue);
        if (actionType == ModerationActionType.BAN_USER && banDurationDays != null && banDurationDays < 1) {
            throw validationError("정지 기간은 1일 이상이어야 합니다.");
        }

        return transactionRunner.required(() -> resolve(reportId, admin, actionType, reason.trim(), banDurationDays));
    }

    private ReportResolutionResult resolve(
            Long reportId, User admin, ModerationActionType actionType, String reason, Integer banDurationDays
    ) {
        Report report = port.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ReportResolutionApplicationException(
                        ReportResolutionErrorCode.REPORT_NOT_FOUND, "존재하지 않는 신고입니다."));
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ReportResolutionApplicationException(
                    ReportResolutionErrorCode.ALREADY_HANDLED, "이미 처리된 신고입니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        switch (actionType) {
            case DELETE_CONTENT -> deleteContent(report, now);
            case BAN_USER -> banOwner(report, admin, reason, banDurationDays, now);
            default -> {
            }
        }

        report.resolve(admin, ReportStatus.RESOLVED, now);
        port.save(report);
        port.saveModerationAction(new ModerationAction(
                report, admin, report.getTargetType(), report.getTargetId(), actionType, reason));

        notifications.record(REPORT_RESOLVED,admin.getId(),report.getId(),null,reason);
        if(actionType==ModerationActionType.WARN_USER)notifications.record(USER_WARNED,admin.getId(),report.getId(),null,reason);
        if(actionType==ModerationActionType.DELETE_CONTENT)notifications.record(CONTENT_MODERATED,admin.getId(),report.getId(),null,reason);
        return new ReportResolutionResult(
                report.getId(), report.getStatus(), actionType,
                report.getHandledAt(), report.getTargetType(), report.getTargetId());
    }

    private void deleteContent(Report report, LocalDateTime now) {
        if (report.getTargetType() == ReportTargetType.SHORT) {
            throw validationError("숏폼 삭제는 지원하지 않습니다. 작성자 정지 또는 다른 조치를 선택해 주세요.");
        }
        contentPort.deleteContent(report.getTargetType(), report.getTargetId(), now);
    }

    private void banOwner(Report report, User admin, String reason, Integer banDurationDays, LocalDateTime now) {
        User owner = contentPort.findOwnerId(report.getTargetType(), report.getTargetId())
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new ReportResolutionApplicationException(
                        ReportResolutionErrorCode.TARGET_NOT_FOUND, "신고 대상의 작성자를 찾을 수 없습니다."));
        if (owner.getRole() == UserRole.ADMIN) {
            throw validationError("관리자는 정지할 수 없습니다.");
        }
        if (userBanRepository.findActiveByUserId(owner.getId(), now).isPresent()) {
            throw new ReportResolutionApplicationException(
                    ReportResolutionErrorCode.ALREADY_BANNED_USER, "이미 정지된 사용자입니다.");
        }
        LocalDateTime bannedUntil = banDurationDays == null ? null : now.plusDays(banDurationDays);
        var savedBan=userBanRepository.save(UserBan.create(owner, admin, reason, bannedUntil));
        notifications.record(USER_BANNED,admin.getId(),savedBan.getId());
    }

    private User requireAdmin(Long adminUserId) {
        return userRepository.findById(adminUserId)
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new ReportResolutionApplicationException(
                        ReportResolutionErrorCode.FORBIDDEN, "관리자만 처리할 수 있습니다."));
    }

    private ModerationActionType parseActionType(String value) {
        try {
            ModerationActionType actionType = ModerationActionType.valueOf(value);
            if (actionType == ModerationActionType.REJECT_REPORT) {
                throw new IllegalArgumentException("reject action must use the reject endpoint");
            }
            if (actionType == ModerationActionType.HIDE_CONTENT) {
                throw new IllegalArgumentException("hide action is not supported");
            }
            return actionType;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw validationError("올바르지 않은 처리 유형입니다.");
        }
    }

    private static ReportResolutionApplicationException validationError(String message) {
        return new ReportResolutionApplicationException(ReportResolutionErrorCode.VALIDATION_ERROR, message);
    }
}
