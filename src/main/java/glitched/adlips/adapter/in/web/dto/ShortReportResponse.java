package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortReportResult;

public record ShortReportResponse(Long reportId, String targetType, Long targetId) {
    public static ShortReportResponse from(ShortReportResult result) {
        return new ShortReportResponse(result.reportId(), "SHORT", result.shortId());
    }
}
