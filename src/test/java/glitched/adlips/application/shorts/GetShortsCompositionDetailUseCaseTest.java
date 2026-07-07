package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.shorts.port.out.ShortsComposerQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsComposersQueryPort;
import glitched.adlips.application.shorts.port.out.ShortsCompositionDetailQueryPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetShortsCompositionDetailUseCaseTest {

    @Test
    void 숏폼이_존재하면_제목과_참여자_목록을_조합해_반환한다() {
        ShortsCompositionDetailQueryPort titlePort = mock(ShortsCompositionDetailQueryPort.class);
        ShortsComposersQueryPort composersPort = mock(ShortsComposersQueryPort.class);
        List<ShortsComposerQueryItem> items = List.of(
                new ShortsComposerQueryItem(3L, "guitar_moon", "https://cdn.example.com/profiles/3.png",
                        "기타", "메인 기타 리프를 만들었습니다."),
                new ShortsComposerQueryItem(7L, "vocal_wave", "https://cdn.example.com/profiles/7.png",
                        "보컬", "후렴 멜로디와 보컬 라인을 추가했습니다.")
        );
        when(titlePort.findActiveShortTitle(12L)).thenReturn(Optional.of("밤하늘 위 멜로디"));
        when(composersPort.findByShortId(12L)).thenReturn(items);
        GetShortsCompositionDetailUseCase useCase =
                new GetShortsCompositionDetailUseCase(titlePort, composersPort);

        ShortsCompositionDetailResult result = useCase.execute(12L);

        assertThat(result.shortId()).isEqualTo(12L);
        assertThat(result.title()).isEqualTo("밤하늘 위 멜로디");
        assertThat(result.details()).isEqualTo(items);
    }

    @Test
    void 참여자가_없어도_빈_리스트로_정상_반환한다() {
        ShortsCompositionDetailQueryPort titlePort = mock(ShortsCompositionDetailQueryPort.class);
        ShortsComposersQueryPort composersPort = mock(ShortsComposersQueryPort.class);
        when(titlePort.findActiveShortTitle(12L)).thenReturn(Optional.of("밤하늘 위 멜로디"));
        when(composersPort.findByShortId(12L)).thenReturn(List.of());
        GetShortsCompositionDetailUseCase useCase =
                new GetShortsCompositionDetailUseCase(titlePort, composersPort);

        ShortsCompositionDetailResult result = useCase.execute(12L);

        assertThat(result.details()).isEmpty();
    }

    @Test
    void 숏폼이_없으면_SHORT_NOT_FOUND_예외가_발생하고_참여자_조회는_생략한다() {
        ShortsCompositionDetailQueryPort titlePort = mock(ShortsCompositionDetailQueryPort.class);
        ShortsComposersQueryPort composersPort = mock(ShortsComposersQueryPort.class);
        when(titlePort.findActiveShortTitle(999L)).thenReturn(Optional.empty());
        GetShortsCompositionDetailUseCase useCase =
                new GetShortsCompositionDetailUseCase(titlePort, composersPort);

        assertThatThrownBy(() -> useCase.execute(999L))
                .isInstanceOf(ShortsCompositionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortsCompositionErrorCode.SHORT_NOT_FOUND);

        verify(composersPort, never()).findByShortId(999L);
    }
}
