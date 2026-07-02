package glitched.adlips.application.shorts;

import glitched.adlips.application.shorts.port.out.ShortPlaybackPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToggleShortPlaybackUseCaseTest {

    @Mock ShortPlaybackPort playbackPort;

    Clock fixedClock = Clock.fixed(Instant.parse("2024-01-15T10:30:45Z"), ZoneOffset.UTC);
    ToggleShortPlaybackUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ToggleShortPlaybackUseCase(playbackPort, fixedClock);
    }

    @Test
    void 유효한_숏폼과_유효한_currentTime으로_토글하면_결과를_반환한다() {
        when(playbackPort.existsActiveShort(12L)).thenReturn(true);

        ShortPlaybackResult result = useCase.toggle(12L, true, 15.5);

        assertThat(result.shortId()).isEqualTo(12L);
        assertThat(result.isPlaying()).isFalse();
        assertThat(result.pausedAt()).isEqualTo(15.5);
        assertThat(result.timestamp()).isEqualTo(Instant.parse("2024-01-15T10:30:45Z"));
    }

    @Test
    void 일시정지된_숏폼을_토글하면_재생_상태를_반환한다() {
        when(playbackPort.existsActiveShort(12L)).thenReturn(true);

        ShortPlaybackResult result = useCase.toggle(12L, false, 15.5);

        assertThat(result.isPlaying()).isTrue();
        assertThat(result.pausedAt()).isEqualTo(15.5);
    }

    @Test
    void 존재하지_않는_숏폼이면_SHORT_NOT_FOUND_예외가_발생한다() {
        when(playbackPort.existsActiveShort(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.toggle(999L, true, 15.5))
                .isInstanceOf(ShortPlaybackApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortPlaybackErrorCode.SHORT_NOT_FOUND);

        verify(playbackPort).existsActiveShort(999L);
    }

    @Test
    void currentTime이_음수이면_INVALID_CURRENT_TIME_예외가_발생한다() {
        assertThatThrownBy(() -> useCase.toggle(12L, true, -1.0))
                .isInstanceOf(ShortPlaybackApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortPlaybackErrorCode.INVALID_CURRENT_TIME);

        verify(playbackPort, never()).existsActiveShort(any());
    }

    @Test
    void currentTime이_유한한_숫자가_아니면_INVALID_CURRENT_TIME_예외가_발생한다() {
        assertThatThrownBy(() -> useCase.toggle(12L, true, null))
                .isInstanceOf(ShortPlaybackApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortPlaybackErrorCode.INVALID_CURRENT_TIME);

        assertThatThrownBy(() -> useCase.toggle(12L, true, Double.NaN))
                .isInstanceOf(ShortPlaybackApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortPlaybackErrorCode.INVALID_CURRENT_TIME);

        assertThatThrownBy(() -> useCase.toggle(12L, true, Double.POSITIVE_INFINITY))
                .isInstanceOf(ShortPlaybackApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortPlaybackErrorCode.INVALID_CURRENT_TIME);

        verify(playbackPort, never()).existsActiveShort(any());
    }

    @Test
    void 재생_상태가_누락되면_INVALID_PLAYBACK_STATE_예외가_발생한다() {
        assertThatThrownBy(() -> useCase.toggle(12L, null, 15.5))
                .isInstanceOf(ShortPlaybackApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortPlaybackErrorCode.INVALID_PLAYBACK_STATE);

        verify(playbackPort, never()).existsActiveShort(any());
    }
}
