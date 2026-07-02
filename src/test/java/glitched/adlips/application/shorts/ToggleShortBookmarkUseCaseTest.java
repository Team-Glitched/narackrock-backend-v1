package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortBookmarkPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToggleShortBookmarkUseCaseTest {

    @Mock ShortBookmarkPort bookmarkPort;
    @Mock TransactionRunner transactionRunner;

    ToggleShortBookmarkUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ToggleShortBookmarkUseCase(bookmarkPort, transactionRunner);
        when(transactionRunner.required(any())).thenAnswer(inv ->
                inv.<Supplier<?>>getArgument(0).get());
    }

    @Test
    void 북마크_없음_상태에서_토글하면_북마크가_추가된다() {
        when(bookmarkPort.lockActiveShort(12L)).thenReturn(true);
        when(bookmarkPort.isBookmarked(1L, 12L)).thenReturn(false);

        ShortBookmarkResult result = useCase.toggle(1L, 12L);

        verify(bookmarkPort).saveBookmark(1L, 12L);
        verify(bookmarkPort, never()).deleteBookmark(any(), any());
        assertThat(result.shortId()).isEqualTo(12L);
        assertThat(result.isBookmarked()).isTrue();
    }

    @Test
    void 북마크_있음_상태에서_토글하면_북마크가_삭제된다() {
        when(bookmarkPort.lockActiveShort(12L)).thenReturn(true);
        when(bookmarkPort.isBookmarked(1L, 12L)).thenReturn(true);

        ShortBookmarkResult result = useCase.toggle(1L, 12L);

        verify(bookmarkPort).deleteBookmark(1L, 12L);
        verify(bookmarkPort, never()).saveBookmark(any(), any());
        assertThat(result.shortId()).isEqualTo(12L);
        assertThat(result.isBookmarked()).isFalse();
    }

    @Test
    void 활성_숏폼이_없으면_북마크를_변경하지_않는다() {
        when(bookmarkPort.lockActiveShort(12L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.toggle(1L, 12L))
                .isInstanceOf(ShortBookmarkApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortBookmarkErrorCode.SHORT_NOT_FOUND);

        verify(bookmarkPort, never()).isBookmarked(any(), any());
        verify(bookmarkPort, never()).saveBookmark(any(), any());
        verify(bookmarkPort, never()).deleteBookmark(any(), any());
    }
}
