package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.domain.community.PostComment;
import java.time.LocalDateTime;

/** 댓글 소프트 삭제와 그에 따른 답글·카운트 정리. 작성자 삭제와 관리 삭제가 같은 규칙을 쓴다. */
final class PostCommentDeletion {

    private PostCommentDeletion() {
    }

    static void delete(PostCommentPort port, Long commentId, PostComment comment, LocalDateTime now) {
        Long postId = comment.getPostId();
        Long parentCommentId = comment.getParentCommentId();
        comment.delete(now);
        port.update(comment);
        int deletedCount = 1;
        if (parentCommentId == null) {
            deletedCount += port.softDeleteActiveReplies(commentId, now);
        }
        port.adjustPostCommentCount(postId, -deletedCount);
        if (parentCommentId != null) {
            port.adjustParentReplyCount(parentCommentId, -1);
        }
    }
}
