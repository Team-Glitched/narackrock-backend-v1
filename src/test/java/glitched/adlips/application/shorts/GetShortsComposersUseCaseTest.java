package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsComposersQueryPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetShortsComposersUseCaseTest {

    @Test
    void 숏폼이_존재하면_참여자_목록을_그대로_반환한다() {
        ShortsComposersQueryPort port = mock(ShortsComposersQueryPort.class);
        List<ShortsComposerQueryItem> items = List.of(
                new ShortsComposerQueryItem(3L, "guitar_moon", "https://cdn.example.com/profiles/3.png",
                        "기타", "메인 기타 리프를 만들었습니다."),
                new ShortsComposerQueryItem(7L, "vocal_wave", "https://cdn.example.com/profiles/7.png",
                        "보컬", "후렴 멜로디와 보컬 라인을 추가했습니다.")
        );
        when(port.existsActiveShort(12L)).thenReturn(true);
        when(port.findByShortId(12L)).thenReturn(items);
        GetShortsComposersUseCase useCase = new GetShortsComposersUseCase(port);

        List<ShortsComposerQueryItem> result = useCase.execute(12L);

        assertThat(result).isEqualTo(items);
    }

    @Test
    void 참여자가_없어도_빈_리스트를_반환한다() {
        ShortsComposersQueryPort port = mock(ShortsComposersQueryPort.class);
        when(port.existsActiveShort(12L)).thenReturn(true);
        when(port.findByShortId(12L)).thenReturn(List.of());
        GetShortsComposersUseCase useCase = new GetShortsComposersUseCase(port);

        List<ShortsComposerQueryItem> result = useCase.execute(12L);

        assertThat(result).isEmpty();
    }

    @Test
    void 숏폼이_없으면_SHORTS_NOT_FOUND_예외가_발생한다() {
        ShortsComposersQueryPort port = mock(ShortsComposersQueryPort.class);
        when(port.existsActiveShort(999L)).thenReturn(false);
        GetShortsComposersUseCase useCase = new GetShortsComposersUseCase(port);

        assertThatThrownBy(() -> useCase.execute(999L))
                .isInstanceOf(ShortsComposersApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortsComposersErrorCode.SHORTS_NOT_FOUND);

        verify(port, never()).findByShortId(999L);
    }
}
