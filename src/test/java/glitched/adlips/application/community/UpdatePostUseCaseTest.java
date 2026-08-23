package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.Post;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdatePostUseCaseTest {

    PostPort port;
    TransactionRunner transactionRunner;
    UpdatePostUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(PostPort.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new UpdatePostUseCase(port, transactionRunner);
    }

    private Post post(Long writerId) {
        return new Post(mock(Gallery.class), User.create("writer@test.com").withId(writerId), "제목", "내용");
    }

    @Test
    void 정상_수정시_postId를_반환한다() {
        when(port.findActivePost(501L, 10L)).thenReturn(Optional.of(post(1L)));

        Long postId = useCase.execute(10L, 501L, 1L, "수정된 제목", "수정된 내용");

        assertThat(postId).isEqualTo(501L);
    }

    @Test
    void 제목이_공백이면_VALIDATION_ERROR가_발생하고_조회하지_않는다() {
        assertThatThrownBy(() -> useCase.execute(10L, 501L, 1L, "  ", "내용"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.VALIDATION_ERROR);

        verify(port, never()).findActivePost(any(), any());
    }

    @Test
    void 존재하지_않으면_POST_NOT_FOUND가_발생한다() {
        when(port.findActivePost(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(10L, 999L, 1L, "제목", "내용"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 본인_게시글이_아니면_NOT_POST_OWNER가_발생하고_수정하지_않는다() {
        when(port.findActivePost(501L, 10L)).thenReturn(Optional.of(post(2L)));

        assertThatThrownBy(() -> useCase.execute(10L, 501L, 1L, "제목", "내용"))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostErrorCode.NOT_POST_OWNER);

        verify(port, never()).update(any());
    }
}
