package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.Post;

public class UpdatePostUseCase {

    private final PostPort port;
    private final TransactionRunner transactionRunner;

    public UpdatePostUseCase(PostPort port, TransactionRunner transactionRunner) {
        this.port = port;
        this.transactionRunner = transactionRunner;
    }

    public Long execute(Long galleryId, Long postId, Long userId, String title, String content) {
        if (title == null || title.isBlank()) {
            throw new PostApplicationException(PostErrorCode.VALIDATION_ERROR, "제목은 필수 입력 사항입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new PostApplicationException(PostErrorCode.VALIDATION_ERROR, "내용은 필수 입력 사항입니다.");
        }

        return transactionRunner.required(() -> {
            Post post = port.findActivePost(postId, galleryId)
                    .orElseThrow(() -> new PostApplicationException(
                            PostErrorCode.POST_NOT_FOUND, "수정하려는 게시글을 찾을 수 없거나 이미 삭제되었습니다."));
            if (!post.getUserId().equals(userId)) {
                throw new PostApplicationException(
                        PostErrorCode.NOT_POST_OWNER, "본인이 작성한 게시글만 수정할 수 있습니다.");
            }
            post.updateTitleAndContent(title.trim(), content.trim());
            port.update(post);
            return postId;
        });
    }
}
