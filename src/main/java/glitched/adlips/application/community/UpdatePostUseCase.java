package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.Post;
import java.time.Clock;
import java.time.LocalDateTime;

public class UpdatePostUseCase {

    private final PostPort port;
    private final UserBanQueryPort userBanQueryPort;
    private final TransactionRunner transactionRunner;

    private final Clock clock;

    public UpdatePostUseCase(
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

    public UpdatePostUseCase(PostPort port, TransactionRunner transactionRunner) {
        this(port, (userId, now) -> false, transactionRunner, Clock.systemUTC());
    }

    public Long execute(Long postId, Long userId, String title, String content) {
        if (userBanQueryPort.isBanned(userId, LocalDateTime.now(clock))) {
            throw new PostApplicationException(
                    PostErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 게시글을 수정할 수 없습니다.");
        }
        if (title == null || title.isBlank()) {
            throw new PostApplicationException(PostErrorCode.INVALID_INPUT_VALUE, "게시글의 제목과 내용은 필수 입력 사항입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new PostApplicationException(PostErrorCode.INVALID_INPUT_VALUE, "게시글의 제목과 내용은 필수 입력 사항입니다.");
        }

        return transactionRunner.required(() -> {
            Post post = port.findActivePost(postId)
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
