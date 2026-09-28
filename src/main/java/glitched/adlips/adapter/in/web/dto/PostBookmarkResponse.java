package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.PostBookmarkResult;

public record PostBookmarkResponse(long postId, boolean isBookmarked) {

    public static PostBookmarkResponse from(PostBookmarkResult result) {
        return new PostBookmarkResponse(result.postId(), result.bookmarked());
    }
}
