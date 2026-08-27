package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.Post;
import java.time.Clock;
import java.time.LocalDateTime;

public class DeletePostUseCase {

    private final PostPort port;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public DeletePostUseCase(PostPort port, TransactionRunner transactionRunner, Clock clock) {
        this.port = port;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public void execute(Long galleryId, Long postId, Long userId) {
        transactionRunner.<Void>required(() -> {
            if (!port.existsGallery(galleryId)) {
                throw new PostApplicationException(
                        PostErrorCode.GALLERY_NOT_FOUND, "존재하지 않는 갤러리입니다.");
            }
            Post post = port.findActivePost(postId, galleryId)
                    .orElseThrow(() -> new PostApplicationException(
                            PostErrorCode.POST_NOT_FOUND, "존재하지 않거나 이미 삭제된 게시글입니다."));
            if (!post.getUserId().equals(userId)) {
                throw new PostApplicationException(
                        PostErrorCode.NOT_POST_OWNER, "본인이 작성한 게시글만 삭제할 수 있습니다.");
            }
            post.delete(LocalDateTime.now(clock));
            port.update(post);
            return null;
        });
    }
}
