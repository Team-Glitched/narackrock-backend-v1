package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortCommentQueryItem;
import java.util.List;

public record ShortCommentListResult(Long shortId, List<ShortCommentQueryItem> comments) {
}
