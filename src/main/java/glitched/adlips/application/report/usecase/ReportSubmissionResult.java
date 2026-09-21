package glitched.adlips.application.report.usecase;

import glitched.adlips.domain.report.ReportTargetType;

public record ReportSubmissionResult(
        Long reportId,
        ReportTargetType targetType,
        Long targetId
) {
}
