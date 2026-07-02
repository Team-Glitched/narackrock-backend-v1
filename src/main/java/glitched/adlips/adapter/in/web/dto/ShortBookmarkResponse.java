package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortBookmarkResult;

public record ShortBookmarkResponse(long shortId, boolean isBookmarked) {

    public static ShortBookmarkResponse from(ShortBookmarkResult result) {
        return new ShortBookmarkResponse(result.shortId(), result.isBookmarked());
    }
}
