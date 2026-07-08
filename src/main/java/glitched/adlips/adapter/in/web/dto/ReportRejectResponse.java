package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.report.usecase.ReportResolutionResult;
import java.time.LocalDateTime;

public record ReportRejectResponse(
        Long reportId,
        String status,
        LocalDateTime handledAt
) {
    public static ReportRejectResponse from(ReportResolutionResult result) {
        return new ReportRejectResponse(
                result.reportId(),
                result.status().name(),
                result.handledAt());
    }
}
