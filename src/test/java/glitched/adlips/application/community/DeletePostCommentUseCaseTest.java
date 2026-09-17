package glitched.adlips.application.community;

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
import glitched.adlips.domain.community.PostComment;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeletePostCommentUseCaseTest {

    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-17T10:00:00Z"), ZoneOffset.UTC);
    static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 17, 10, 0);

    PostCommentPort port;
    UserBanQueryPort userBanQueryPort;
    TransactionRunner transactionRunner;
    DeletePostCommentUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(PostCommentPort.class);
        userBanQueryPort = mock(UserBanQueryPort.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new DeletePostCommentUseCase(port, userBanQueryPort, transactionRunner, FIXED_CLOCK);
    }

    PostComment comment(Long userId, Long parentCommentId) {
        PostComment comment = mock(PostComment.class);
        when(comment.getUserId()).thenReturn(userId);
        when(comment.getPostId()).thenReturn(10L);
        when(comment.getParentCommentId()).thenReturn(parentCommentId);
        return comment;
    }

    @Test
    void 본인_댓글을_삭제하면_삭제시각을_남기고_게시글_댓글수를_1_줄인다() {
        PostComment comment = comment(1L, null);
        when(port.findActiveComment(7721L)).thenReturn(Optional.of(comment));

        useCase.execute(7721L, 1L);

        verify(comment).delete(NOW);
        verify(port).update(comment);
        verify(port).adjustPostCommentCount(10L, -1);
        verify(port, never()).adjustParentReplyCount(any(), anyInt());
    }

    @Test
    void 대댓글을_삭제하면_부모_댓글의_답글수도_1_줄인다() {
        PostComment comment = comment(1L, 7000L);
        when(port.findActiveComment(7722L)).thenReturn(Optional.of(comment));

        useCase.execute(7722L, 1L);

        verify(port).adjustPostCommentCount(10L, -1);
        verify(port).adjustParentReplyCount(7000L, -1);
    }

    @Test
    void 정지된_사용자는_BANNED_USER_ACCESS_예외가_발생하고_댓글을_조회하지_않는다() {
        when(userBanQueryPort.isBanned(1L, NOW)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(7721L, 1L))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("현재 서비스 이용 정지 상태이므로 댓글을 삭제할 수 없습니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.BANNED_USER_ACCESS);

        verify(port, never()).findActiveComment(any());
    }

    @Test
    void 존재하지_않거나_이미_삭제된_댓글이면_COMMENT_NOT_FOUND_예외가_발생한다() {
        when(port.findActiveComment(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(404L, 1L))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("존재하지 않거나 이미 삭제된 댓글입니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.COMMENT_NOT_FOUND);

        verify(port, never()).update(any());
        verify(port, never()).adjustPostCommentCount(any(), anyInt());
    }

    @Test
    void 작성자가_아니면_NOT_COMMENT_OWNER_예외가_발생하고_삭제하지_않는다() {
        PostComment comment = comment(2L, null);
        when(port.findActiveComment(7721L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> useCase.execute(7721L, 1L))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("본인이 작성한 댓글만 삭제할 수 있습니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.NOT_COMMENT_OWNER);

        verify(comment, never()).delete(any());
        verify(port, never()).update(any());
        verify(port, never()).adjustPostCommentCount(any(), anyInt());
    }
}
