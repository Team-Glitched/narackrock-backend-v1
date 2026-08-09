package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortCommentReportResult;

public record ShortCommentReportResponse(Long reportId, String targetType, Long targetId) {
    public static ShortCommentReportResponse from(ShortCommentReportResult result) {
        return new ShortCommentReportResponse(result.reportId(), "SHORT_COMMENT", result.commentId());
    }
}
