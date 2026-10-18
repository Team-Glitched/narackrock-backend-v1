package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.GalleryActor;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.Post;
import java.time.Clock;
import java.time.LocalDateTime;

public class ModeratePostDeletionUseCase {

    private final GalleryAuthorityPort authorityPort;
    private final PostPort postPort;
    private final GalleryModerationLogPort logPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public ModeratePostDeletionUseCase(
            GalleryAuthorityPort authorityPort,
            PostPort postPort,
            GalleryModerationLogPort logPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.authorityPort = authorityPort;
        this.postPort = postPort;
        this.logPort = logPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public void execute(Long actorId, Long galleryId, Long postId, String reason) {
        transactionRunner.<Void>required(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            GalleryModerationGuard.requireGallery(authorityPort, galleryId);
            GalleryActor actor = GalleryModerationGuard.requireManager(authorityPort, actorId, galleryId, now);
            GalleryModerationGuard.requireReason(reason);
            Post post = postPort.findActivePost(postId, galleryId)
                    .orElseThrow(() -> new GalleryModerationApplicationException(
                            GalleryModerationErrorCode.POST_NOT_FOUND, "이 갤러리에 존재하지 않거나 이미 삭제된 게시글입니다."));
            GalleryModerationGuard.requireCanDeleteContentOf(authorityPort, actor, post.getUserId(), galleryId, now);

            post.delete(now);
            postPort.update(post);
            logPort.save(galleryId, actorId, GalleryModerationActionType.DELETE_POST,
                    post.getUserId(), postId, reason.trim());
            return null;
        });
    }
}
