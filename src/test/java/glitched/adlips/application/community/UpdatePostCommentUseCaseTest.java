package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

class UpdatePostCommentUseCaseTest {

    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-17T10:00:00Z"), ZoneOffset.UTC);
    static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 17, 10, 0);

    PostCommentPort port;
    UserBanQueryPort userBanQueryPort;
    TransactionRunner transactionRunner;
    UpdatePostCommentUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(PostCommentPort.class);
        userBanQueryPort = mock(UserBanQueryPort.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new UpdatePostCommentUseCase(port, userBanQueryPort, transactionRunner, FIXED_CLOCK);
    }

    PostComment ownedBy(Long userId) {
        PostComment comment = mock(PostComment.class);
        when(comment.getUserId()).thenReturn(userId);
        return comment;
    }

    @Test
    void 본인_댓글을_수정하면_commentId를_반환하고_내용을_반영한다() {
        PostComment comment = ownedBy(1L);
        when(port.findActiveComment(7721L)).thenReturn(Optional.of(comment));

        Long updated = useCase.execute(7721L, 1L, "수정된 댓글 내용입니다.");

        assertThat(updated).isEqualTo(7721L);
        verify(comment).updateContent("수정된 댓글 내용입니다.");
        verify(port).update(comment);
    }

    @Test
    void 내용의_앞뒤_공백은_제거하고_반영한다() {
        PostComment comment = ownedBy(1L);
        when(port.findActiveComment(7721L)).thenReturn(Optional.of(comment));

        useCase.execute(7721L, 1L, "  수정된 댓글 내용입니다.  ");

        verify(comment).updateContent("수정된 댓글 내용입니다.");
    }

    @Test
    void 정지된_사용자는_BANNED_USER_ACCESS_예외가_발생하고_댓글을_조회하지_않는다() {
        when(userBanQueryPort.isBanned(1L, NOW)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(7721L, 1L, "수정된 댓글 내용입니다."))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("현재 서비스 이용 정지 상태이므로 댓글을 수정할 수 없습니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.BANNED_USER_ACCESS);

        verify(port, never()).findActiveComment(any());
    }

    @Test
    void 정지_확인은_입력_검증보다_먼저_수행한다() {
        when(userBanQueryPort.isBanned(1L, NOW)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(7721L, 1L, "   "))
                .isInstanceOf(PostCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.BANNED_USER_ACCESS);
    }

    @Test
    void 내용이_공백이면_INVALID_INPUT_VALUE_예외가_발생하고_댓글을_조회하지_않는다() {
        assertThatThrownBy(() -> useCase.execute(7721L, 1L, "   "))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("댓글 내용은 필수 입력 사항입니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.INVALID_INPUT_VALUE);

        verify(port, never()).findActiveComment(any());
    }

    @Test
    void 내용이_null이면_INVALID_INPUT_VALUE_예외가_발생한다() {
        assertThatThrownBy(() -> useCase.execute(7721L, 1L, null))
                .isInstanceOf(PostCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 존재하지_않거나_삭제된_댓글이면_COMMENT_NOT_FOUND_예외가_발생한다() {
        when(port.findActiveComment(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(404L, 1L, "수정된 댓글 내용입니다."))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("수정하려는 댓글을 찾을 수 없거나 이미 삭제되었습니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.COMMENT_NOT_FOUND);

        verify(port, never()).update(any());
    }

    @Test
    void 작성자가_아니면_NOT_COMMENT_OWNER_예외가_발생하고_수정하지_않는다() {
        PostComment comment = ownedBy(2L);
        when(port.findActiveComment(7721L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> useCase.execute(7721L, 1L, "수정된 댓글 내용입니다."))
                .isInstanceOf(PostCommentApplicationException.class)
                .hasMessage("본인이 작성한 댓글만 수정할 수 있습니다.")
                .extracting("errorCode")
                .isEqualTo(PostCommentErrorCode.NOT_COMMENT_OWNER);

        verify(comment, never()).updateContent(any());
        verify(port, never()).update(any());
    }
}
