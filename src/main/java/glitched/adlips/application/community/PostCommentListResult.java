package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostCommentQueryItem;
import java.util.List;

public record PostCommentListResult(Long postId, Long parentCommentId, int page, int size,
        long totalCount, List<PostCommentQueryItem> comments) {}
