package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortsCompositionDetailResult;
import java.util.List;

public record ShortsCompositionDetailResponse(
        Long shortId,
        String title,
        List<ShortsComposerResponse> details
) {
    public static ShortsCompositionDetailResponse from(ShortsCompositionDetailResult result) {
        return new ShortsCompositionDetailResponse(
                result.shortId(),
                result.title(),
                result.details().stream().map(ShortsComposerResponse::from).toList());
    }
}
