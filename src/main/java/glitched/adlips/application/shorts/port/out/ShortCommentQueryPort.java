package glitched.adlips.application.shorts.port.out;

import java.util.List;

public interface ShortCommentQueryPort {
    boolean existsActiveShort(Long shortId);

    List<ShortCommentQueryItem> findComments(Long shortId, Long viewerId);
}
