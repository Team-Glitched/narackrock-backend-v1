package glitched.adlips.application.report.usecase;

import glitched.adlips.domain.report.ModerationActionType;
import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.report.ReportTargetType;
import java.time.LocalDateTime;

public record ReportResolutionResult(
        Long reportId,
        ReportStatus status,
        ModerationActionType actionType,
        LocalDateTime handledAt,
        ReportTargetType targetType,
        Long targetId
) {
}
