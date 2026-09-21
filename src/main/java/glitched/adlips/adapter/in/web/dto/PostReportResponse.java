package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.report.usecase.ReportSubmissionResult;
import glitched.adlips.domain.report.ReportTargetType;

public record PostReportResponse(
        Long reportId,
        ReportTargetType targetType,
        Long targetId
) {
    public static PostReportResponse from(ReportSubmissionResult result) {
        return new PostReportResponse(
                result.reportId(), result.targetType(), result.targetId());
    }
}
