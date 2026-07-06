package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;

public record ShortsComposerResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String role,
        String description
) {
    public static ShortsComposerResponse from(ShortsComposerQueryItem item) {
        return new ShortsComposerResponse(
                item.userId(), item.nickname(), item.profileImageUrl(), item.role(), item.description());
    }
}
