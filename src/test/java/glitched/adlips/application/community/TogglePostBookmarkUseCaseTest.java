package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostBookmarkPort;
import glitched.adlips.application.port.TransactionRunner;
import org.junit.jupiter.api.Test;

class TogglePostBookmarkUseCaseTest {

    final PostBookmarkPort port = mock(PostBookmarkPort.class);
    final TogglePostBookmarkUseCase useCase = new TogglePostBookmarkUseCase(port, TransactionRunner.direct());

    @Test
    void 북마크가_없으면_추가한다() {
        when(port.lockActivePost(10L)).thenReturn(true);
        when(port.isBookmarked(1L, 10L)).thenReturn(false);

        PostBookmarkResult result = useCase.toggle(1L, 10L);

        verify(port).saveBookmark(1L, 10L);
        assertThat(result).isEqualTo(new PostBookmarkResult(10L, true));
    }

    @Test
    void 북마크가_있으면_삭제한다() {
        when(port.lockActivePost(10L)).thenReturn(true);
        when(port.isBookmarked(1L, 10L)).thenReturn(true);

        PostBookmarkResult result = useCase.toggle(1L, 10L);

        verify(port).deleteBookmark(1L, 10L);
        verify(port, never()).saveBookmark(any(), any());
        assertThat(result).isEqualTo(new PostBookmarkResult(10L, false));
    }

    @Test
    void 없거나_삭제된_게시글이면_POST_NOT_FOUND() {
        when(port.lockActivePost(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.toggle(1L, 999L))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode").isEqualTo(PostErrorCode.POST_NOT_FOUND);
        verify(port, never()).saveBookmark(any(), any());
    }
}
