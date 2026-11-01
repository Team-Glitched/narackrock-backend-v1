package glitched.adlips.application.opensource;

import glitched.adlips.application.port.TransactionRunner;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenSourceQueryUseCaseTest {
    final OpenSourceQueryPort port = mock(OpenSourceQueryPort.class);
    final OpenSourceQueryUseCase useCase = new OpenSourceQueryUseCase(port, TransactionRunner.direct());

    @Test void normalizesKeywordAndFetchesOneExtraForHasNext() {
        when(port.list(1L, "guitar", 2, 3)).thenReturn(Arrays.asList(null, null, null));
        var page = useCase.list(1L, " guitar ", 1, 2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.items()).hasSize(2);
        verify(port).list(1L, "guitar", 2, 3);
    }
    @Test void emptyKeywordIsUnfiltered() {
        when(port.list(1L, null, 0, 21)).thenReturn(List.of());
        assertThat(useCase.list(1L, "  ", 0, 20).hasNext()).isFalse();
        verify(port).list(1L, null, 0, 21);
    }
    @Test void rejectsInvalidPageSizeOverflowAndKeyword() {
        for(int[] args : new int[][] {{-1,20},{0,0},{0,101},{Integer.MAX_VALUE,100}}) {
            assertThatThrownBy(() -> useCase.list(1L,null,args[0],args[1])).isInstanceOf(OpenSourceException.class);
            assertThatThrownBy(() -> useCase.saved(1L,args[0],args[1])).isInstanceOf(OpenSourceException.class);
            assertThatThrownBy(() -> useCase.publications(1L,args[0],args[1])).isInstanceOf(OpenSourceException.class);
        }
        assertThatThrownBy(() -> useCase.list(1L,"x".repeat(101),0,20)).isInstanceOf(OpenSourceException.class);
        verifyNoInteractions(port);
    }
    @Test void unavailableDetailUses404AndInvalidIdsNeverQuery() {
        when(port.detail(1L,10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.detail(1L,10L)).isInstanceOf(OpenSourceException.class)
            .satisfies(e -> assertThat(((OpenSourceException)e).getCode()).isEqualTo(OpenSourceException.Code.OPEN_SOURCE_NOT_FOUND));
        assertThatThrownBy(() -> useCase.detail(1L,0L)).isInstanceOf(OpenSourceException.class);
        verify(port,never()).detail(1L,0L);
    }
}
