package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostReactionPort;
import glitched.adlips.application.community.port.out.ReactionCounts;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.community.port.out.GalleryBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.reaction.ReactionToggle;
import glitched.adlips.domain.reaction.ReactionType;
import java.time.Clock;
import java.time.LocalDateTime;

public class TogglePostReactionUseCase {

    private final PostReactionPort reactionPort;
    private final UserBanQueryPort userBanQueryPort;
    private final GalleryBanQueryPort galleryBanQueryPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public TogglePostReactionUseCase(
            PostReactionPort reactionPort,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this(reactionPort, userBanQueryPort, GalleryBanQueryPort.none(), transactionRunner, clock);
    }

    public TogglePostReactionUseCase(
            PostReactionPort reactionPort,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.reactionPort = reactionPort;
        this.userBanQueryPort = userBanQueryPort;
        this.galleryBanQueryPort = galleryBanQueryPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public PostReactionResult toggle(Long userId, Long postId, ReactionType requested) {
        if (userBanQueryPort.isBanned(userId, LocalDateTime.now(clock))) {
            throw new PostApplicationException(
                    PostErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 게시글에 반응할 수 없습니다.");
        }
        return transactionRunner.required(() -> {
            if (!reactionPort.lockActivePost(postId)) {
                throw new PostApplicationException(PostErrorCode.POST_NOT_FOUND, "존재하지 않거나 삭제된 게시글입니다.");
            }
            if (galleryBanQueryPort.isRestrictedForPost(postId, userId, LocalDateTime.now(clock))) {
                throw new PostApplicationException(PostErrorCode.GALLERY_RESTRICTED_USER,
                        "이 갤러리에서 이용이 제한된 상태이므로 게시글에 반응할 수 없습니다.");
            }
            ReactionType current = reactionPort.findReaction(userId, postId).orElse(null);
            ReactionToggle toggle = ReactionToggle.of(current, requested);
            if (toggle.result() == null) {
                reactionPort.deleteReaction(userId, postId);
            } else {
                reactionPort.saveReaction(userId, postId, toggle.result());
            }
            reactionPort.adjustReactionCounts(postId, toggle.likeDelta(), toggle.dislikeDelta());
            ReactionCounts counts = reactionPort.findReactionCounts(postId);
            return new PostReactionResult(
                    postId,
                    toggle.result() == ReactionType.LIKE,
                    toggle.result() == ReactionType.DISLIKE,
                    counts.likeCount(),
                    counts.dislikeCount());
        });
    }
}
