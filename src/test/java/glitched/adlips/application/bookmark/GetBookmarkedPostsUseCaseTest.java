package glitched.adlips.application.bookmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import glitched.adlips.application.bookmark.port.out.BookmarkQueryPort;
import glitched.adlips.application.bookmark.port.out.BookmarkedPostItem;
import glitched.adlips.application.port.TransactionRunner;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetBookmarkedPostsUseCaseTest {

    final BookmarkQueryPort port = mock(BookmarkQueryPort.class);
    final GetBookmarkedPostsUseCase useCase = new GetBookmarkedPostsUseCase(port, TransactionRunner.direct());

    @Test
    void 내가_북마크한_게시글과_전체_수를_반환한다() {
        BookmarkedPostItem item = new BookmarkedPostItem(
                501L, 10L, "음악", 2L, "writer", null, "제목", "내용", null, 3, 1, 0,
                LocalDateTime.of(2026, 9, 20, 10, 0), LocalDateTime.of(2026, 9, 27, 10, 0));
        when(port.findBookmarkedPosts(1L, 0, 20)).thenReturn(List.of(item));
        when(port.countBookmarkedPosts(1L)).thenReturn(1L);

        BookmarkedPostListResult result = useCase.execute(1L, 0, 20);

        assertThat(result).isEqualTo(new BookmarkedPostListResult(0, 20, 1L, List.of(item)));
    }

    @Test
    void 페이지_번호와_크기가_잘못되면_조회하지_않고_INVALID_INPUT_VALUE() {
        for (int[] values : new int[][]{{-1, 20}, {0, 0}, {0, 101}}) {
            assertThatThrownBy(() -> useCase.execute(1L, values[0], values[1]))
                    .isInstanceOf(BookmarkApplicationException.class)
                    .extracting("errorCode").isEqualTo(BookmarkErrorCode.INVALID_INPUT_VALUE);
        }
        verifyNoInteractions(port);
    }
}
