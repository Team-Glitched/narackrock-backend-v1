package glitched.adlips.application.report.port.out;

import glitched.adlips.domain.report.ReportTargetType;
import java.time.LocalDateTime;

public record ReportQueryItem(
        Long reportId,
        ReportTargetType targetType,
        Long targetId,
        String targetUrl,
        boolean targetDeleted,
        String reason,
        String description,
        LocalDateTime reportedAt,
        ReportUserSummary reporter,
        ReportUserSummary reportedUser,
        String contentSummary
) {
}
