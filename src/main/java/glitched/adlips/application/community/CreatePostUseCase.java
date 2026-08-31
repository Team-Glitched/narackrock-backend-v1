package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import java.time.LocalDateTime;

public class CreatePostUseCase {

    private final PostPort port;
    private final UserBanQueryPort userBanQueryPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public CreatePostUseCase(
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

    public CreatePostUseCase(PostPort port, TransactionRunner transactionRunner) {
        this(port, (userId, now) -> false, transactionRunner, Clock.systemUTC());
    }

    public PostResult execute(Long galleryId, Long userId, String title, String content) {
        if (userBanQueryPort.isBanned(userId, LocalDateTime.now(clock))) {
            throw new PostApplicationException(
                    PostErrorCode.BANNED_USER_ACCESS,
                    "현재 서비스 이용 정지 상태이므로 게시글을 생성할 수 없습니다.");
        }
        if (title == null || title.isBlank()) {
            throw new PostApplicationException(PostErrorCode.VALIDATION_ERROR, "제목은 필수 입력 사항입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new PostApplicationException(PostErrorCode.VALIDATION_ERROR, "내용은 필수 입력 사항입니다.");
        }

        return transactionRunner.required(() -> {
            if (!port.existsGallery(galleryId)) {
                throw new PostApplicationException(
                        PostErrorCode.GALLERY_NOT_FOUND, "존재하지 않는 갤러리에는 게시글을 작성할 수 없습니다.");
            }
            Long postId = port.save(galleryId, userId, title.trim(), content.trim());
            return new PostResult(postId, galleryId);
        });
    }
}
