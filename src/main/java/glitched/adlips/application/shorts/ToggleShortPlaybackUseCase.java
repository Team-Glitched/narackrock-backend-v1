package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortPlaybackPort;

import java.time.Clock;
import java.time.Instant;

public class ToggleShortPlaybackUseCase {

    private final ShortPlaybackPort playbackPort;
    private final Clock clock;

    public ToggleShortPlaybackUseCase(ShortPlaybackPort playbackPort, Clock clock) {
        this.playbackPort = playbackPort;
        this.clock = clock;
    }

    public ShortPlaybackResult toggle(Long shortId, double currentTime) {
        if (currentTime < 0) {
            throw new ShortPlaybackApplicationException(
                    ShortPlaybackErrorCode.INVALID_CURRENT_TIME,
                    "currentTime은 0 이상의 숫자여야 합니다.");
        }
        if (!playbackPort.existsActiveShort(shortId)) {
            throw new ShortPlaybackApplicationException(
                    ShortPlaybackErrorCode.SHORT_NOT_FOUND,
                    "존재하지 않는 숏폼입니다.");
        }
        return new ShortPlaybackResult(shortId, false, currentTime, Instant.now(clock));
    }
}
