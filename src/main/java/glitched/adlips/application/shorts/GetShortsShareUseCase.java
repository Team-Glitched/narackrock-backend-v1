package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortsShareLinkPort;
import glitched.adlips.application.shorts.port.out.ShortsSharePort;

public class GetShortsShareUseCase {

    private final ShortsSharePort sharePort;
    private final ShortsShareLinkPort linkPort;

    public GetShortsShareUseCase(ShortsSharePort sharePort, ShortsShareLinkPort linkPort) {
        this.sharePort = sharePort;
        this.linkPort = linkPort;
    }

    public ShortsShareResult execute(Long shortId) {
        if (!sharePort.existsActiveShort(shortId)) {
            throw new ShortsCompositionApplicationException(
                    ShortsCompositionErrorCode.SHORT_NOT_FOUND, "존재하지 않는 숏폼입니다.");
        }
        return new ShortsShareResult(shortId, linkPort.create(shortId));
    }
}
