package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.community.PostResult;

public record PostCreateResponse(Long postId) {
    public static PostCreateResponse from(PostResult result) {
        return new PostCreateResponse(result.postId());
    }
}
