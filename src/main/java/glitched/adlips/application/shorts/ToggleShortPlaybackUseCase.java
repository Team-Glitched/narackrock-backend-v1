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

    public ShortPlaybackResult toggle(Long shortId, Boolean isPlaying, Double currentTime) {
        if (isPlaying == null) {
            throw new ShortPlaybackApplicationException(
                    ShortPlaybackErrorCode.INVALID_PLAYBACK_STATE,
                    "isPlaying은 필수입니다.");
        }
        if (currentTime == null || !Double.isFinite(currentTime) || currentTime < 0) {
            throw new ShortPlaybackApplicationException(
                    ShortPlaybackErrorCode.INVALID_CURRENT_TIME,
                    "currentTime은 0 이상의 유한한 숫자여야 합니다.");
        }
        if (!playbackPort.existsActiveShort(shortId)) {
            throw new ShortPlaybackApplicationException(
                    ShortPlaybackErrorCode.SHORT_NOT_FOUND,
                    "존재하지 않는 숏폼입니다.");
        }
        return new ShortPlaybackResult(shortId, !isPlaying, currentTime, Instant.now(clock));
    }
}
