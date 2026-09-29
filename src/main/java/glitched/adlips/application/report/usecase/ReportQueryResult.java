package glitched.adlips.application.report.usecase;

import glitched.adlips.application.report.port.out.ReportQueryItem;
import java.util.List;

public record ReportQueryResult(
        List<ReportQueryItem> content,
        Long nextCursor,
        boolean hasNext
) {
}
