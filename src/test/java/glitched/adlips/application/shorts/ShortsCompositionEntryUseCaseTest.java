package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryItem;
import glitched.adlips.application.shorts.port.out.ShortsCompositionQueryPort;
import glitched.adlips.domain.project.ProjectStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortsCompositionEntryUseCaseTest {

    @Mock ShortsCompositionQueryPort queryPort;

    ShortsCompositionEntryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ShortsCompositionEntryUseCase(queryPort);
    }

    @Test
    void 프로젝트가_연결된_숏폼이면_조회_결과를_그대로_반환한다() {
        ShortsCompositionQueryItem item = new ShortsCompositionQueryItem(
                12L, 8L, "밤하늘 위 멜로디", ProjectStatus.IN_PROGRESS, null);
        when(queryPort.findByShortId(12L)).thenReturn(Optional.of(item));

        ShortsCompositionQueryItem result = useCase.execute(12L);

        assertThat(result).isEqualTo(item);
    }

    @Test
    void 숏폼이_없으면_SHORT_NOT_FOUND_예외가_발생한다() {
        when(queryPort.findByShortId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(999L))
                .isInstanceOf(ShortsCompositionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortsCompositionErrorCode.SHORT_NOT_FOUND);
    }

    @Test
    void 프로젝트가_연결되지_않은_숏폼이면_PROJECT_NOT_LINKED_예외가_발생한다() {
        ShortsCompositionQueryItem item = new ShortsCompositionQueryItem(
                12L, null, null, null, null);
        when(queryPort.findByShortId(12L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> useCase.execute(12L))
                .isInstanceOf(ShortsCompositionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortsCompositionErrorCode.PROJECT_NOT_LINKED);
    }

    @Test
    void 연결된_프로젝트가_삭제되었으면_PROJECT_NOT_LINKED_예외가_발생한다() {
        ShortsCompositionQueryItem item = new ShortsCompositionQueryItem(
                12L, 8L, "밤하늘 위 멜로디", ProjectStatus.IN_PROGRESS, LocalDateTime.now());
        when(queryPort.findByShortId(12L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> useCase.execute(12L))
                .isInstanceOf(ShortsCompositionApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortsCompositionErrorCode.PROJECT_NOT_LINKED);
    }
}
