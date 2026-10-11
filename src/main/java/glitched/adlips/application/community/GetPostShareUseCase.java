package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.PostShareLinkPort;
import glitched.adlips.application.community.port.out.PostSharePort;

public class GetPostShareUseCase {

    private final PostSharePort sharePort;
    private final PostShareLinkPort linkPort;

    public GetPostShareUseCase(PostSharePort sharePort, PostShareLinkPort linkPort) {
        this.sharePort = sharePort;
        this.linkPort = linkPort;
    }

    public PostShareResult execute(Long postId) {
        if (!sharePort.existsActivePost(postId)) {
            throw new PostApplicationException(PostErrorCode.POST_NOT_FOUND, "존재하지 않거나 삭제된 게시글입니다.");
        }
        return new PostShareResult(postId, linkPort.create(postId));
    }
}
