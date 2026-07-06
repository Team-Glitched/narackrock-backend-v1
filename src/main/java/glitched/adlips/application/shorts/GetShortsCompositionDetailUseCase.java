package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsComposersQueryPort;
import glitched.adlips.application.shorts.port.out.ShortsCompositionDetailQueryPort;
import java.util.List;

public class GetShortsCompositionDetailUseCase {

    private final ShortsCompositionDetailQueryPort titlePort;
    private final ShortsComposersQueryPort composersPort;

    public GetShortsCompositionDetailUseCase(
            ShortsCompositionDetailQueryPort titlePort,
            ShortsComposersQueryPort composersPort
    ) {
        this.titlePort = titlePort;
        this.composersPort = composersPort;
    }

    public ShortsCompositionDetailResult execute(Long shortId) {
        String title = titlePort.findActiveShortTitle(shortId)
                .orElseThrow(() -> new ShortsCompositionApplicationException(
                        ShortsCompositionErrorCode.SHORT_NOT_FOUND, "존재하지 않는 숏폼입니다."));
        List<ShortsComposerQueryItem> details = composersPort.findByShortId(shortId);
        return new ShortsCompositionDetailResult(shortId, title, details);
    }
}
