package glitched.adlips.application.community.port.out;

import java.util.List;

public interface PostCommentQueryPort {
    boolean existsActivePost(Long postId);
    boolean existsActiveParentComment(Long parentCommentId, Long postId);
    List<PostCommentQueryItem> findComments(Long postId, Long parentCommentId, int page, int size);
    long countComments(Long postId, Long parentCommentId);
}
