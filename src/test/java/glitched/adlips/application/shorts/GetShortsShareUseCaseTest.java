package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.shorts.port.out.ShortsShareLinkPort;
import glitched.adlips.application.shorts.port.out.ShortsSharePort;
import org.junit.jupiter.api.Test;

class GetShortsShareUseCaseTest {

    @Test
    void 숏폼이_존재하면_공유_링크를_반환한다() {
        ShortsSharePort sharePort = mock(ShortsSharePort.class);
        ShortsShareLinkPort linkPort = mock(ShortsShareLinkPort.class);
        when(sharePort.existsActiveShort(12L)).thenReturn(true);
        when(linkPort.create(12L)).thenReturn("https://app.example.com/shorts/12");
        GetShortsShareUseCase useCase = new GetShortsShareUseCase(sharePort, linkPort);

        ShortsShareResult result = useCase.execute(12L);

        assertThat(result.shortId()).isEqualTo(12L);
        assertThat(result.shareUrl()).isEqualTo("https://app.example.com/shorts/12");
    }

    @Test
    void 숏폼이_없으면_SHORT_NOT_FOUND_예외가_발생하고_링크는_조합하지_않는다() {
        ShortsSharePort sharePort = mock(ShortsSharePort.class);
        ShortsShareLinkPort linkPort = mock(ShortsShareLinkPort.class);
        when(sharePort.existsActiveShort(999L)).thenReturn(false);
        GetShortsShareUseCase useCase = new GetShortsShareUseCase(sharePort, linkPort);

        assertThatThrownBy(() -> useCase.execute(999L))
                .isInstanceOf(ShortsCompositionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortsCompositionErrorCode.SHORT_NOT_FOUND);

        verify(linkPort, never()).create(999L);
    }
}
