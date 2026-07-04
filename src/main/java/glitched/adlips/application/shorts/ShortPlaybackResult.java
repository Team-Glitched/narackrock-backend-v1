package glitched.adlips.application.shorts;

import java.time.Instant;

public record ShortPlaybackResult(
        long shortId,
        boolean isPlaying,
        double pausedAt,
        Instant timestamp
) {}
