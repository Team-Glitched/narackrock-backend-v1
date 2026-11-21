package glitched.adlips.application.opensource;

import glitched.adlips.application.port.TransactionRunner;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenSourceSaveUseCaseTest {
    final OpenSourceSavePort port = mock(OpenSourceSavePort.class);
    final OpenSourceSaveUseCase useCase = new OpenSourceSaveUseCase(port, TransactionRunner.direct());

    @Test void savesAvailableSource() {
        when(port.lockAvailableSource(10L)).thenReturn(true);
        assertThat(useCase.save(1L, 10L)).isEqualTo(new OpenSourceSaveResult(10L, true));
        verify(port).insertSave(1L, 10L);
    }
    @Test void repeatedSavePreservesOriginalSave() {
        when(port.lockAvailableSource(10L)).thenReturn(true);
        when(port.isSaved(1L, 10L)).thenReturn(true);
        assertThat(useCase.save(1L, 10L).isSaved()).isTrue();
        verify(port, never()).insertSave(anyLong(), anyLong());
    }
    @Test void unavailableSourceCannotBeSaved() {
        assertThatThrownBy(() -> useCase.save(1L, 10L))
            .isInstanceOf(OpenSourceException.class).hasMessageContaining("샘플");
        verify(port, never()).insertSave(anyLong(), anyLong());
    }
    @Test void removalDoesNotRequireAvailableSource() {
        assertThat(useCase.remove(1L, 10L).isSaved()).isFalse();
        verify(port).deleteSave(1L, 10L);
        verify(port, never()).lockAvailableSource(anyLong());
    }
    @Test void invalidIdsDoNotReachPersistence() {
        assertThatThrownBy(() -> useCase.save(1L, 0L)).isInstanceOf(OpenSourceException.class);
        assertThatThrownBy(() -> useCase.remove(1L, -1L)).isInstanceOf(OpenSourceException.class);
        verifyNoInteractions(port);
    }
}
