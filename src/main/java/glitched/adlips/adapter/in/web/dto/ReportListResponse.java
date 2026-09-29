package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.report.port.out.ReportQueryItem;
import glitched.adlips.application.report.port.out.ReportUserSummary;
import glitched.adlips.application.report.usecase.ReportQueryResult;
import glitched.adlips.domain.report.ReportTargetType;
import java.time.LocalDateTime;
import java.util.List;

public record ReportListResponse(
        List<ReportItemResponse> content,
        Long nextCursor,
        boolean hasNext
) {
    public static ReportListResponse from(ReportQueryResult result) {
        return new ReportListResponse(
                result.content().stream().map(ReportItemResponse::from).toList(),
                result.nextCursor(),
                result.hasNext()
        );
    }

    public record ReportItemResponse(
            Long reportId,
            ReportTargetType targetType,
            Long targetId,
            String targetUrl,
            boolean targetDeleted,
            String reason,
            String description,
            LocalDateTime reportedAt,
            ReportUserResponse reporter,
            ReportUserResponse reportedUser,
            String contentSummary
    ) {
        private static ReportItemResponse from(ReportQueryItem item) {
            return new ReportItemResponse(
                    item.reportId(),
                    item.targetType(),
                    item.targetId(),
                    item.targetUrl(),
                    item.targetDeleted(),
                    item.reason(),
                    item.description(),
                    item.reportedAt(),
                    ReportUserResponse.from(item.reporter()),
                    ReportUserResponse.from(item.reportedUser()),
                    item.contentSummary()
            );
        }
    }

    public record ReportUserResponse(Long userId, String nickname) {
        private static ReportUserResponse from(ReportUserSummary user) {
            return user == null ? null : new ReportUserResponse(user.userId(), user.nickname());
        }
    }
}
