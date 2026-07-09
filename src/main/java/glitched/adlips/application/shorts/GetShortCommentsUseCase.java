package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortCommentQueryPort;

public class GetShortCommentsUseCase {

    private final ShortCommentQueryPort port;

    public GetShortCommentsUseCase(ShortCommentQueryPort port) {
        this.port = port;
    }

    public ShortCommentListResult execute(Long shortId, Long viewerId) {
        if (!port.existsActiveShort(shortId)) {
            throw new ShortCommentApplicationException(
                    ShortCommentErrorCode.SHORT_NOT_FOUND, "존재하지 않는 숏폼입니다.");
        }
        return new ShortCommentListResult(shortId, port.findComments(shortId, viewerId));
    }
}
