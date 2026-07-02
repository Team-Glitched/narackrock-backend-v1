package glitched.adlips.adapter.in.web.dto;

import glitched.adlips.application.shorts.ShortPlaybackResult;

public record ShortPlaybackResponse(
        long shortId,
        boolean isPlaying,
        double pausedAt,
        String timestamp
) {
    public static ShortPlaybackResponse from(ShortPlaybackResult result) {
        return new ShortPlaybackResponse(
                result.shortId(),
                result.isPlaying(),
                result.pausedAt(),
                result.timestamp().toString()
        );
    }
}
