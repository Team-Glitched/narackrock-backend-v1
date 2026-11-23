package glitched.adlips.application.report.usecase;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.report.port.out.ReportSubmissionPort;
import glitched.adlips.application.report.port.out.ReportTarget;
import glitched.adlips.application.report.port.out.ReportTargetQueryPort;
import glitched.adlips.application.report.port.out.ReportUserRestrictionPort;
import glitched.adlips.domain.common.ContentLimits;
import glitched.adlips.domain.report.ReportTargetType;
import java.time.Clock;
import java.time.LocalDateTime;

public class SubmitReportUseCase {
    private final NotificationEvents notifications;

    private final ReportTargetQueryPort targetQueryPort;
    private final ReportSubmissionPort submissionPort;
    private final ReportUserRestrictionPort restrictionPort;
    private final Clock clock;
    private final TransactionRunner transactionRunner;

    public SubmitReportUseCase(
            ReportTargetQueryPort targetQueryPort,
            ReportSubmissionPort submissionPort,
            ReportUserRestrictionPort restrictionPort,
            Clock clock
    ) {
        this(targetQueryPort, submissionPort, restrictionPort, clock, TransactionRunner.direct());
    }

    public SubmitReportUseCase(
            ReportTargetQueryPort targetQueryPort,
            ReportSubmissionPort submissionPort,
            ReportUserRestrictionPort restrictionPort,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        this(targetQueryPort, submissionPort, restrictionPort, clock, transactionRunner, NotificationEvents.none());
    }

    public SubmitReportUseCase(ReportTargetQueryPort targetQueryPort,
            ReportSubmissionPort submissionPort,
            ReportUserRestrictionPort restrictionPort,
            Clock clock,
            TransactionRunner transactionRunner, NotificationEvents notifications) {
        this.targetQueryPort = targetQueryPort;
        this.submissionPort = submissionPort;
        this.restrictionPort = restrictionPort;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
    
        this.notifications = notifications;
    }

    public ReportSubmissionResult execute(
            ReportTargetType targetType,
            Long targetId,
            Long reporterId,
            String reason,
            String description
    ) {
        return transactionRunner.required(() -> submit(
                targetType, targetId, reporterId, reason, description));
    }

    private ReportSubmissionResult submit(
            ReportTargetType targetType,
            Long targetId,
            Long reporterId,
            String reason,
            String description
    ) {
        validateTargetType(targetType);
        String normalizedReason = requireReason(reason);
        String normalizedDescription = normalize(description);
        if (ContentLimits.exceeds(normalizedDescription, ContentLimits.REPORT_DESCRIPTION_MAX_LENGTH)) {
            throw new ReportSubmissionApplicationException(
                    ReportSubmissionErrorCode.VALIDATION_ERROR,
                    "신고 상세 내용은 " + ContentLimits.REPORT_DESCRIPTION_MAX_LENGTH + "자 이내로 입력해 주세요."
            );
        }
        if (restrictionPort.isBanned(reporterId, LocalDateTime.now(clock))) {
            throw new ReportSubmissionApplicationException(
                    ReportSubmissionErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 신고할 수 없습니다."
            );
        }

        ReportTarget target = targetQueryPort.findActiveTarget(targetType, targetId)
                .orElseThrow(() -> notFound(targetType));
        if (target.ownerId().equals(reporterId)) {
            throw ownTarget(targetType);
        }
        if (submissionPort.exists(reporterId, targetType, targetId)) {
            throw alreadyReported(targetType);
        }

        Long reportId = submissionPort.save(
                        reporterId,
                        targetType,
                        targetId,
                        normalizedReason,
                        normalizedDescription
                )
                .orElseThrow(() -> alreadyReported(targetType));
        notifications.record(REPORT_CREATED,reporterId,reportId);
        return new ReportSubmissionResult(reportId, targetType, targetId);
    }

    private void validateTargetType(ReportTargetType targetType) {
        if (targetType != ReportTargetType.POST
                && targetType != ReportTargetType.POST_COMMENT
                && targetType != ReportTargetType.OPEN_SOURCE
                && targetType != ReportTargetType.OPEN_SOURCE_COMMENT) {
            throw new ReportSubmissionApplicationException(
                    ReportSubmissionErrorCode.VALIDATION_ERROR,
                    "지원하지 않는 신고 대상입니다."
            );
        }
    }

    private String requireReason(String reason) {
        String trimmed = glitched.adlips.domain.common.TextSanitizer.sanitizeSingleLine(reason);
        if (trimmed.isEmpty()) {
            throw new ReportSubmissionApplicationException(
                    ReportSubmissionErrorCode.VALIDATION_ERROR,
                    "신고 사유를 입력해 주세요."
            );
        }
        if (ContentLimits.exceeds(trimmed, ContentLimits.REPORT_REASON_MAX_LENGTH)) {
            throw new ReportSubmissionApplicationException(
                    ReportSubmissionErrorCode.VALIDATION_ERROR,
                    "신고 사유는 " + ContentLimits.REPORT_REASON_MAX_LENGTH + "자 이내로 입력해 주세요."
            );
        }
        return trimmed;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = glitched.adlips.domain.common.TextSanitizer.sanitizeMultiline(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private ReportSubmissionApplicationException notFound(ReportTargetType targetType) {
        if (targetType == ReportTargetType.POST) {
            return new ReportSubmissionApplicationException(
                    ReportSubmissionErrorCode.POST_NOT_FOUND,
                    "존재하지 않거나 이미 삭제된 게시글은 신고할 수 없습니다."
            );
        }
        if (targetType == ReportTargetType.OPEN_SOURCE) {
            return new ReportSubmissionApplicationException(
                    ReportSubmissionErrorCode.OPEN_SOURCE_NOT_FOUND,
                    "존재하지 않거나 비공개이거나 삭제된 샘플은 신고할 수 없습니다."
            );
        }
        return new ReportSubmissionApplicationException(
                ReportSubmissionErrorCode.COMMENT_NOT_FOUND,
                "존재하지 않거나 이미 삭제된 댓글은 신고할 수 없습니다."
        );
    }

    private ReportSubmissionApplicationException ownTarget(ReportTargetType targetType) {
        if (targetType == ReportTargetType.POST) {
            return new ReportSubmissionApplicationException(
                    ReportSubmissionErrorCode.CANNOT_REPORT_OWN_POST,
                    "본인이 작성한 게시글은 신고할 수 없습니다."
            );
        }
        if (targetType == ReportTargetType.OPEN_SOURCE) {
            return new ReportSubmissionApplicationException(
                    ReportSubmissionErrorCode.CANNOT_REPORT_OWN_OPEN_SOURCE,
                    "본인이 등록한 샘플은 신고할 수 없습니다."
            );
        }
        return new ReportSubmissionApplicationException(
                ReportSubmissionErrorCode.CANNOT_REPORT_OWN_COMMENT,
                "본인이 작성한 댓글은 신고할 수 없습니다."
        );
    }

    private ReportSubmissionApplicationException alreadyReported(ReportTargetType targetType) {
        String targetName = switch (targetType) {
            case POST -> "게시글";
            case OPEN_SOURCE -> "샘플";
            default -> "댓글";
        };
        return new ReportSubmissionApplicationException(
                ReportSubmissionErrorCode.ALREADY_REPORTED,
                "이미 신고한 " + targetName + "입니다."
        );
    }
}
