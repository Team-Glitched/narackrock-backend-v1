package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortsShareResult;

public record ShortsShareResponse(Long shortId, String shareUrl) {
    public static ShortsShareResponse from(ShortsShareResult result) {
        return new ShortsShareResponse(result.shortId(), result.shareUrl());
    }
}
