package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortLikeResult;

public record ShortLikeResponse(long shortId, boolean isLiked, int likeCount) {

    public static ShortLikeResponse from(ShortLikeResult result) {
        return new ShortLikeResponse(result.shortId(), result.isLiked(), result.likeCount());
    }
}
