package glitched.adlips.application.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.view.port.out.ContentViewCounterPort;
import glitched.adlips.application.view.port.out.ContentViewExistencePort;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordContentViewUseCaseTest {

    private static final Duration DEDUPLICATION_WINDOW = Duration.ofMinutes(30);

    @Mock ContentViewExistencePort existencePort;
    @Mock ContentViewCounterPort counterPort;

    RecordContentViewUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecordContentViewUseCase(existencePort, counterPort, DEDUPLICATION_WINDOW);
    }

    @Test
    void 처음_조회한_콘텐츠면_조회수를_집계한다() {
        when(existencePort.exists(ContentViewTarget.SHORT, 12L)).thenReturn(true);
        when(counterPort.recordIfFirst(ContentViewTarget.SHORT, 12L, "user:7", DEDUPLICATION_WINDOW))
                .thenReturn(true);

        ContentViewResult result = useCase.execute(ContentViewTarget.SHORT, 12L, "user:7");

        assertThat(result.target()).isEqualTo(ContentViewTarget.SHORT);
        assertThat(result.contentId()).isEqualTo(12L);
        assertThat(result.counted()).isTrue();
    }

    @Test
    void 중복_조회면_조회수를_집계하지_않는다() {
        when(existencePort.exists(ContentViewTarget.POST, 501L)).thenReturn(true);
        when(counterPort.recordIfFirst(ContentViewTarget.POST, 501L, "device:hash", DEDUPLICATION_WINDOW))
                .thenReturn(false);

        ContentViewResult result = useCase.execute(ContentViewTarget.POST, 501L, "device:hash");

        assertThat(result.counted()).isFalse();
    }

    @Test
    void 존재하지_않는_콘텐츠면_예외가_발생한다() {
        when(existencePort.exists(ContentViewTarget.SHORT, 999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(ContentViewTarget.SHORT, 999L, "user:7"))
                .isInstanceOf(ContentViewApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ContentViewErrorCode.CONTENT_NOT_FOUND);

        verify(counterPort, never()).recordIfFirst(
                ContentViewTarget.SHORT, 999L, "user:7", DEDUPLICATION_WINDOW);
    }

    @Test
    void 조회자_식별자가_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(ContentViewTarget.POST, 501L, " "))
                .isInstanceOf(ContentViewApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ContentViewErrorCode.VIEWER_ID_REQUIRED);

        verify(existencePort, never()).exists(ContentViewTarget.POST, 501L);
    }

    @Test
    void 콘텐츠_ID가_양수가_아니면_예외가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(ContentViewTarget.POST, 0L, "user:7"))
                .isInstanceOf(ContentViewApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ContentViewErrorCode.INVALID_CONTENT_ID);

        verify(existencePort, never()).exists(ContentViewTarget.POST, 0L);
    }
}
