package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.PostShareResult;

public record PostShareResponse(Long postId, String shareUrl) {

    public static PostShareResponse from(PostShareResult result) {
        return new PostShareResponse(result.postId(), result.shareUrl());
    }
}
