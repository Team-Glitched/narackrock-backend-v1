package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsComposersQueryPort;
import java.util.List;

public class GetShortsComposersUseCase {

    private final ShortsComposersQueryPort queryPort;

    public GetShortsComposersUseCase(ShortsComposersQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public List<ShortsComposerQueryItem> execute(Long shortId) {
        if (!queryPort.existsActiveShort(shortId)) {
            throw new ShortsComposersApplicationException(
                    ShortsComposersErrorCode.SHORTS_NOT_FOUND, "해당 곡을 찾을 수 없습니다.");
        }
        return queryPort.findByShortId(shortId);
    }
}
