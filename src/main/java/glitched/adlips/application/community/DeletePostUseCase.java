package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.Post;
import java.time.Clock;
import java.time.LocalDateTime;

public class DeletePostUseCase {

    private final PostPort port;
    private final UserBanQueryPort userBanQueryPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public DeletePostUseCase(
            PostPort port,
            UserBanQueryPort userBanQueryPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.port = port;
        this.userBanQueryPort = userBanQueryPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public DeletePostUseCase(PostPort port, TransactionRunner transactionRunner, Clock clock) {
        this(port, (userId, now) -> false, transactionRunner, clock);
    }

    public void execute(Long postId, Long userId) {
        if (userBanQueryPort.isBanned(userId, LocalDateTime.now(clock))) {
            throw new PostApplicationException(
                    PostErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 게시글을 삭제할 수 없습니다.");
        }
        transactionRunner.<Void>required(() -> {
            Post post = port.findActivePost(postId)
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
