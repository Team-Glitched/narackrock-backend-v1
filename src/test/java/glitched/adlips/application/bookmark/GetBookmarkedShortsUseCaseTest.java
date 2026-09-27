package glitched.adlips.application.bookmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import glitched.adlips.application.bookmark.port.out.BookmarkQueryPort;
import glitched.adlips.application.bookmark.port.out.BookmarkedShortItem;
import glitched.adlips.application.port.TransactionRunner;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetBookmarkedShortsUseCaseTest {

    final BookmarkQueryPort port = mock(BookmarkQueryPort.class);
    final GetBookmarkedShortsUseCase useCase = new GetBookmarkedShortsUseCase(port, TransactionRunner.direct());

    @Test
    void 다음_페이지가_있으면_size개만_반환하고_마지막_북마크_id를_커서로_준다() {
        when(port.findBookmarkedShorts(1L, null, 3)).thenReturn(List.of(item(30L), item(20L), item(10L)));

        BookmarkedShortPage page = useCase.execute(1L, null, 2);

        assertThat(page.items()).extracting(BookmarkedShortItem::bookmarkId).containsExactly(30L, 20L);
        assertThat(page.nextCursor()).isEqualTo(20L);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.size()).isEqualTo(2);
    }

    @Test
    void 마지막_페이지면_hasMore가_false다() {
        when(port.findBookmarkedShorts(1L, 20L, 3)).thenReturn(List.of(item(10L)));

        BookmarkedShortPage page = useCase.execute(1L, 20L, 2);

        assertThat(page.items()).hasSize(1);
        assertThat(page.nextCursor()).isEqualTo(10L);
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void 북마크가_없으면_빈_페이지와_null_커서를_반환한다() {
        when(port.findBookmarkedShorts(1L, null, 21)).thenReturn(List.of());

        BookmarkedShortPage page = useCase.execute(1L, null, 20);

        assertThat(page).isEqualTo(new BookmarkedShortPage(List.of(), null, false, 20));
    }

    @Test
    void 크기가_잘못되면_조회하지_않고_INVALID_INPUT_VALUE() {
        for (int size : new int[]{0, 101}) {
            assertThatThrownBy(() -> useCase.execute(1L, null, size))
                    .isInstanceOf(BookmarkApplicationException.class)
                    .extracting("errorCode").isEqualTo(BookmarkErrorCode.INVALID_INPUT_VALUE);
        }
        verifyNoInteractions(port);
    }

    private static BookmarkedShortItem item(long bookmarkId) {
        return new BookmarkedShortItem(bookmarkId, bookmarkId + 100, "숏폼", null, "COMPLETED",
                2L, "writer", null, 0, 0L, LocalDateTime.of(2026, 9, 27, 10, 0));
    }
}
