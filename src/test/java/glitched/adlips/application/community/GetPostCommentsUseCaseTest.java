package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import glitched.adlips.application.community.port.out.PostCommentQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetPostCommentsUseCaseTest {
    final PostCommentQueryPort port = mock(PostCommentQueryPort.class);
    final GetPostCommentsUseCase useCase = new GetPostCommentsUseCase(port, TransactionRunner.direct());

    @Test void listsRootsWithTotal() {
        when(port.existsActivePost(1L)).thenReturn(true);
        when(port.findComments(1L, null, 0, 20)).thenReturn(List.of());
        when(port.countComments(1L, null)).thenReturn(3L);
        assertThat(useCase.execute(1L, null, 0, 20).totalCount()).isEqualTo(3);
        verify(port).findComments(1L, null, 0, 20);
    }
    @Test void rejectsMissingPost() {
        assertThatThrownBy(() -> useCase.execute(1L, null, 0, 20))
            .isInstanceOfSatisfying(PostCommentApplicationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(PostCommentErrorCode.POST_NOT_FOUND));
    }
    @Test void rejectsMissingOrOtherPostsParent() {
        when(port.existsActivePost(1L)).thenReturn(true);
        assertThatThrownBy(() -> useCase.execute(1L, 2L, 0, 20))
            .isInstanceOfSatisfying(PostCommentApplicationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(PostCommentErrorCode.PARENT_COMMENT_NOT_FOUND));
    }
    @Test void validatesPaginationBeforeQuerying() {
        for (int[] values : new int[][]{{-1,20},{0,0},{0,101},{Integer.MAX_VALUE,20}}) {
            assertThatThrownBy(() -> useCase.execute(1L, null, values[0], values[1]))
                .isInstanceOf(PostCommentApplicationException.class);
        }
        verifyNoInteractions(port);
    }
    @Test void listsReplies() {
        when(port.existsActivePost(1L)).thenReturn(true);
        when(port.existsActiveParentComment(2L, 1L)).thenReturn(true);
        when(port.findComments(1L, 2L, 1, 10)).thenReturn(List.of());
        useCase.execute(1L, 2L, 1, 10);
        verify(port).findComments(1L, 2L, 1, 10);
    }
}
