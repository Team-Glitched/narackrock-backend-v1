package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.community.port.out.GalleryBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import java.time.Clock;
import java.time.LocalDateTime;

public class CreatePostUseCase {

    private final PostPort port;
    private final UserBanQueryPort userBanQueryPort;
    private final GalleryBanQueryPort galleryBanQueryPort;
    private final MediaFileRepositoryPort mediaFileRepository;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public CreatePostUseCase(
            PostPort port,
            UserBanQueryPort userBanQueryPort,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this(port, userBanQueryPort, GalleryBanQueryPort.none(), mediaFileRepository, transactionRunner, clock);
    }

    public CreatePostUseCase(
            PostPort port,
            UserBanQueryPort userBanQueryPort,
            GalleryBanQueryPort galleryBanQueryPort,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.port = port;
        this.userBanQueryPort = userBanQueryPort;
        this.galleryBanQueryPort = galleryBanQueryPort;
        this.mediaFileRepository = mediaFileRepository;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public CreatePostUseCase(
            PostPort port,
            MediaFileRepositoryPort mediaFileRepository,
            TransactionRunner transactionRunner
    ) {
        this(port, (userId, now) -> false, mediaFileRepository, transactionRunner, Clock.systemUTC());
    }

    public PostResult execute(Long galleryId, Long userId, String title, String content, Long imageFileId) {
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
            if (galleryBanQueryPort.isRestrictedInGallery(galleryId, userId, LocalDateTime.now(clock))) {
                throw new PostApplicationException(PostErrorCode.GALLERY_RESTRICTED_USER,
                        "이 갤러리에서 이용이 제한된 상태이므로 게시글을 작성할 수 없습니다.");
            }
            validateImage(imageFileId, userId);
            Long postId = port.save(galleryId, userId, title.trim(), content.trim(), imageFileId);
            return new PostResult(postId, galleryId);
        });
    }

    public PostResult execute(Long galleryId, Long userId, String title, String content) {
        return execute(galleryId, userId, title, content, null);
    }

    private void validateImage(Long imageFileId, Long userId) {
        if (imageFileId == null) {
            return;
        }
        MediaFile image = mediaFileRepository.findById(imageFileId)
                .orElseThrow(this::invalidImage);
        if (!image.getOwnerId().equals(userId)
                || image.getFileType() != MediaFileType.IMAGE
                || image.getStatus() != MediaFileStatus.READY) {
            throw invalidImage();
        }
    }

    private PostApplicationException invalidImage() {
        return new PostApplicationException(
                PostErrorCode.INVALID_INPUT_VALUE,
                "업로드가 완료된 본인 이미지 파일만 게시글에 첨부할 수 있습니다.");
    }
}
