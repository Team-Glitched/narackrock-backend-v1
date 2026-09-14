package glitched.adlips.application.shorts.port.out;

import glitched.adlips.domain.shorts.ShortComment;
import java.util.Optional;

public interface ShortCommentPort {
    boolean existsActiveShort(Long shortId);

    boolean existsActiveParentComment(Long parentCommentId, Long shortId);

    Long save(Long shortId, Long userId, String content, Long parentCommentId);

    void adjustShortCommentCount(Long shortId, int delta);

    void adjustParentReplyCount(Long parentCommentId, int delta);

    Optional<ShortComment> findActiveComment(Long commentId, Long shortId);

    void updateContent(ShortComment comment);
}
