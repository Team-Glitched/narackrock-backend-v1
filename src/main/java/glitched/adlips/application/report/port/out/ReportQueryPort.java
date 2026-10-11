package glitched.adlips.application.report.port.out;

import glitched.adlips.domain.report.ReportStatus;
import glitched.adlips.domain.report.ReportTargetType;
import java.util.List;

public interface ReportQueryPort {
    List<ReportQueryItem> findReports(
            ReportStatus status,
            ReportTargetType targetType,
            Long cursor,
            int limit
    );
}
