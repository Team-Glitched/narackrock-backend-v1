package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;
import java.util.List;

public record ShortsCompositionDetailResult(
        Long shortId,
        String title,
        List<ShortsComposerQueryItem> details
) {
}
