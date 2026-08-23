package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostQueryItem;
import glitched.adlips.application.community.port.out.PostQueryPort;

public class GetPostUseCase {

    private final PostQueryPort port;

    public GetPostUseCase(PostQueryPort port) {
        this.port = port;
    }

    public PostQueryItem execute(Long galleryId, Long postId) {
        if (!port.existsGallery(galleryId)) {
            throw new PostApplicationException(PostErrorCode.GALLERY_NOT_FOUND, "존재하지 않는 갤러리입니다.");
        }
        return port.findDetail(postId, galleryId)
                .orElseThrow(() -> new PostApplicationException(
                        PostErrorCode.POST_NOT_FOUND, "존재하지 않거나 이미 삭제된 게시글입니다."));
    }
}
