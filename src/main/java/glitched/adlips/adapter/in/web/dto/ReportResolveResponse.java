package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.report.usecase.ReportResolutionResult;
import java.time.LocalDateTime;

public record ReportResolveResponse(
        Long reportId,
        String status,
        String actionType,
        LocalDateTime handledAt,
        String targetType,
        Long targetId
) {
    public static ReportResolveResponse from(ReportResolutionResult result) {
        return new ReportResolveResponse(
                result.reportId(),
                result.status().name(),
                result.actionType().name(),
                result.handledAt(),
                result.targetType().name(),
                result.targetId());
    }
}
