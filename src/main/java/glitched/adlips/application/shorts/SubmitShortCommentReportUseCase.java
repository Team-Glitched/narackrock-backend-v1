package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.application.shorts.port.out.ShortCommentReportPort;
import glitched.adlips.domain.shorts.ShortComment;

public class SubmitShortCommentReportUseCase {

    private final ShortCommentPort shortCommentPort;
    private final ShortCommentReportPort reportPort;

    public SubmitShortCommentReportUseCase(
            ShortCommentPort shortCommentPort,
            ShortCommentReportPort reportPort
    ) {
        this.shortCommentPort = shortCommentPort;
        this.reportPort = reportPort;
    }

    public ShortCommentReportResult execute(
            Long shortId, Long commentId, Long reporterId, String reason, String description
    ) {
        if (reason == null || reason.isBlank()) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.INVALID_INPUT_VALUE, "신고 사유를 입력해 주세요.");
        }
        ShortComment comment = shortCommentPort.findActiveComment(commentId, shortId)
                .orElseThrow(() -> new ShortCommentApplicationException(
                        ShortCommentErrorCode.COMMENT_NOT_FOUND,
                        "존재하지 않거나 이미 삭제된 댓글은 신고할 수 없습니다."));
        if (comment.getUserId().equals(reporterId)) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.CANNOT_REPORT_OWN_COMMENT, "본인이 작성한 댓글은 신고할 수 없습니다.");
        }
        if (reportPort.existsByReporterAndComment(reporterId, commentId)) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.ALREADY_REPORTED, "이미 신고한 댓글입니다.");
        }
        Long reportId = reportPort.save(reporterId, commentId, reason.trim(), normalize(description))
                .orElseThrow(() -> new ShortCommentApplicationException(
                        ShortCommentErrorCode.ALREADY_REPORTED, "이미 신고한 댓글입니다."));
        return new ShortCommentReportResult(reportId, commentId);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
