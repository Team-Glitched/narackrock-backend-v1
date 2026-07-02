package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortDislikeResult;

public record ShortDislikeResponse(long shortId, boolean isDisliked, int dislikeCount) {

    public static ShortDislikeResponse from(ShortDislikeResult result) {
        return new ShortDislikeResponse(result.shortId(), result.isDisliked(), result.dislikeCount());
    }
}
