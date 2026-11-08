package glitched.adlips.application.shorts;

import glitched.adlips.application.notification.NotificationEvents;
import static glitched.adlips.domain.notification.NotificationType.*;

import glitched.adlips.application.shorts.port.out.ShortReportPort;

public class SubmitShortReportUseCase {
    private final NotificationEvents notifications;
    private final glitched.adlips.application.port.TransactionRunner transactionRunner;

    private final ShortReportPort port;

    public SubmitShortReportUseCase(ShortReportPort port) {
        this(port, glitched.adlips.application.port.TransactionRunner.direct());
    }
    public SubmitShortReportUseCase(ShortReportPort port, glitched.adlips.application.port.TransactionRunner transactionRunner) {
        this(port, transactionRunner, NotificationEvents.none());
    }

    public SubmitShortReportUseCase(ShortReportPort port, glitched.adlips.application.port.TransactionRunner transactionRunner, NotificationEvents notifications) {
        this.port = port;
    
        this.transactionRunner=transactionRunner;
    
        this.notifications = notifications;
    }

    public ShortReportResult execute(Long shortId, Long reporterId, String reason, String description) {return transactionRunner.required(()->executeInternal(shortId, reporterId, reason, description));}

    private ShortReportResult executeInternal(Long shortId, Long reporterId, String reason, String description) {
        if (reason == null || reason.isBlank()) {
            throw new ShortReportApplicationException(
                    ShortReportErrorCode.VALIDATION_ERROR, "신고 사유를 입력해 주세요.");
        }
        Long ownerId = port.findActiveShortOwnerId(shortId)
                .orElseThrow(() -> new ShortReportApplicationException(
                        ShortReportErrorCode.SHORT_NOT_FOUND, "존재하지 않는 숏폼입니다."));
        if (ownerId.equals(reporterId)) {
            throw new ShortReportApplicationException(
                    ShortReportErrorCode.CANNOT_REPORT_OWN_SHORT, "본인이 작곡한 숏폼은 신고할 수 없습니다.");
        }
        if (port.existsByReporterAndShort(reporterId, shortId)) {
            throw new ShortReportApplicationException(
                    ShortReportErrorCode.ALREADY_REPORTED, "이미 신고한 숏폼입니다.");
        }
        Long reportId = port.save(reporterId, shortId, reason.trim(), normalize(description))
                .orElseThrow(() -> new ShortReportApplicationException(
                        ShortReportErrorCode.ALREADY_REPORTED, "이미 신고한 숏폼입니다."));
        notifications.record(REPORT_CREATED,reporterId,reportId);
        return new ShortReportResult(reportId, shortId);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
