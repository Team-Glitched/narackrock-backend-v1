package glitched.adlips.application.shorts;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.application.shorts.port.out.ShortCommentReportPort;
import glitched.adlips.domain.common.ContentLimits;
import glitched.adlips.domain.shorts.ShortComment;

public class SubmitShortCommentReportUseCase {
    private final NotificationEvents notifications;
    private final glitched.adlips.application.port.TransactionRunner transactionRunner;

    private final ShortCommentPort shortCommentPort;
    private final ShortCommentReportPort reportPort;

    public SubmitShortCommentReportUseCase(
            ShortCommentPort shortCommentPort,
            ShortCommentReportPort reportPort
    ) {
        this(shortCommentPort, reportPort, glitched.adlips.application.port.TransactionRunner.direct());
    }
    public SubmitShortCommentReportUseCase(
            ShortCommentPort shortCommentPort,
            ShortCommentReportPort reportPort
    , glitched.adlips.application.port.TransactionRunner transactionRunner) {
        this(shortCommentPort, reportPort, transactionRunner, NotificationEvents.none());
    }

    public SubmitShortCommentReportUseCase(ShortCommentPort shortCommentPort,
            ShortCommentReportPort reportPort
    , glitched.adlips.application.port.TransactionRunner transactionRunner, NotificationEvents notifications) {
        this.shortCommentPort = shortCommentPort;
        this.reportPort = reportPort;
    
        this.transactionRunner=transactionRunner;
    
        this.notifications = notifications;
    }

    public ShortCommentReportResult execute(
            Long shortId, Long commentId, Long reporterId, String reason, String description
    ) {return transactionRunner.required(()->executeInternal(shortId, commentId, reporterId, reason, description));}

    private ShortCommentReportResult executeInternal(
            Long shortId, Long commentId, Long reporterId, String reason, String description
    ) {
        if (reason == null || reason.isBlank()) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.INVALID_INPUT_VALUE, "신고 사유를 입력해 주세요.");
        }
        String trimmedReason = reason.trim();
        if (ContentLimits.exceeds(trimmedReason, ContentLimits.REPORT_REASON_MAX_LENGTH)) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.INVALID_INPUT_VALUE,
                    "신고 사유는 " + ContentLimits.REPORT_REASON_MAX_LENGTH + "자 이내로 입력해 주세요.");
        }
        String normalizedDescription = normalize(description);
        if (ContentLimits.exceeds(normalizedDescription, ContentLimits.REPORT_DESCRIPTION_MAX_LENGTH)) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.INVALID_INPUT_VALUE,
                    "신고 상세 내용은 " + ContentLimits.REPORT_DESCRIPTION_MAX_LENGTH + "자 이내로 입력해 주세요.");
        }
        ShortComment comment = shortCommentPort.findActiveComment(commentId, shortId)
                .orElseThrow(() -> new ShortCommentApplicationException(
                        ShortCommentErrorCode.COMMENT_NOT_FOUND,
                        "존재하지 않거나 이미 삭제된 댓글은 신고할 수 없습니다."));
        if (comment.getUserId().equals(reporterId)) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.CANNOT_REPORT_OWN_COMMENT, "본인이 작성한 댓글은 신고할 수 없습니다.");
        }
        if (reportPort.existsByReporterAndComment(reporterId, commentId)) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.ALREADY_REPORTED, "이미 신고한 댓글입니다.");
        }
        Long reportId = reportPort.save(reporterId, commentId, trimmedReason, normalizedDescription)
                .orElseThrow(() -> new ShortCommentApplicationException(
                        ShortCommentErrorCode.ALREADY_REPORTED, "이미 신고한 댓글입니다."));
        notifications.record(REPORT_CREATED,reporterId,reportId);
        return new ShortCommentReportResult(reportId, commentId);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
