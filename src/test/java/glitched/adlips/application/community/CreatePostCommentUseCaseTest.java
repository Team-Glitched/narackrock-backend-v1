package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostCommentPort;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreatePostCommentUseCaseTest {

    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-17T10:00:00Z"), ZoneOffset.UTC);
    static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 17, 10, 0);

    PostCommentPort port;
    UserBanQueryPort userBanQueryPort;
    TransactionRunner transactionRunner;
    CreatePostCommentUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(PostCommentPort.class);
        userBanQueryPort = mock(UserBanQueryPort.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new CreatePostCommentUseCase(port, userBanQueryPort, transactionRunner, FIXED_CLOCK);
    }

    @Test
    void 댓글을_등록하면_commentId를_반환하고_게시글_댓글수를_1_증가시킨다() {
        when(port.existsActivePost(10L)).thenReturn(true);
        when(port.save(10L, 1L, "좋은 곡이네요", null)).thenReturn(7721L);

        PostCommentResult result = useCase.execute(10L, 1L, "좋은 곡이네요", null);

        assertThat(result.postId()).isEqualTo(10L);
        assertThat(result.commentId()).isEqualTo(7721L);
        assertThat(result.parentCommentId()).isNull();
        verify(port).adjustPostCommentCount(10L, 1);
        verify(port, never()).adjustParentReplyCount(any(), anyInt());
    }

    @Test
    void 내용의_앞뒤_공백은_제거하고_저장한다() {
        when(port.existsActivePost(10L)).thenReturn(true);
        when(port.save(10L, 1L, "좋은 곡이네요", null)).thenReturn(7721L);

        useCase.execute(10L, 1L, "  좋은 곡이네요  ", null);

        verify(port).save(10L, 1L, "좋은 곡이네요", null);
    }

    @Test
    void 대댓글을_등록하면_부모_댓글의_답글수도_1_증가시킨다() {
        when(port.existsActivePost(10L)).thenReturn(true);
        when(port.existsActiveParentComment(7721L, 10L)).thenReturn(true);
        when(port.save(10L, 1L, "동의합니다", 7721L)).thenReturn(7722L);

        PostCommentResult result = useCase.execute(10L, 1L, "동의합니다", 7721L);

        assertThat(result.commentId()).isEqualTo(7722L);
        assertThat(result.parentCommentId()).isEqualTo(7721L);
        verify(port).adjustPostCommentCount(10L, 1);
        verify(port).adjustParentReplyCount(7721L, 1);
    }

    @Test
    void 정지된_사용자는_BANNED_USER_ACCESS_예외가_발생하고_저장하지_않는다() {
        when(userBanQueryPort.isBanned(1L, NOW)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(10L, 1L, "좋은 곡이네요", null))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("현재 서비스 이용 정지 상태이므로 댓글을 작성할 수 없습니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.BANNED_USER_ACCESS);

        verify(port, never()).save(any(), any(), any(), any());
    }

    @Test
    void 정지_확인은_입력_검증보다_먼저_수행한다() {
        when(userBanQueryPort.isBanned(1L, NOW)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(10L, 1L, "   ", null))
                .isInstanceOf(PostCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.BANNED_USER_ACCESS);
    }

    @Test
    void 내용이_공백이면_INVALID_INPUT_VALUE_예외가_발생하고_게시글을_조회하지_않는다() {
        assertThatThrownBy(() -> useCase.execute(10L, 1L, "   ", null))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("댓글 내용은 필수 입력 사항입니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.INVALID_INPUT_VALUE);

        verify(port, never()).existsActivePost(any());
    }

    @Test
    void 내용이_null이면_INVALID_INPUT_VALUE_예외가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(10L, 1L, null, null))
                .isInstanceOf(PostCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 대댓글_내용이_공백이면_대댓글_안내_문구로_예외가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(10L, 1L, "   ", 7721L))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("대댓글 내용은 필수 입력 사항입니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 존재하지_않거나_삭제된_게시글이면_POST_NOT_FOUND_예외가_발생하고_저장하지_않는다() {
        when(port.existsActivePost(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(999L, 1L, "좋은 곡이네요", null))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("존재하지 않거나 삭제된 게시글에는 댓글을 작성할 수 없습니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.POST_NOT_FOUND);

        verify(port, never()).save(any(), any(), any(), any());
        verify(port, never()).adjustPostCommentCount(any(), anyInt());
    }

    @Test
    void 존재하지_않거나_삭제된_부모_댓글이면_PARENT_COMMENT_NOT_FOUND_예외가_발생한다() {
        when(port.existsActivePost(10L)).thenReturn(true);
        when(port.existsActiveParentComment(404L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(10L, 1L, "동의합니다", 404L))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("존재하지 않거나 삭제된 댓글에는 대댓글을 작성할 수 없습니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.PARENT_COMMENT_NOT_FOUND);

        verify(port, never()).save(any(), any(), any(), any());
    }
}
