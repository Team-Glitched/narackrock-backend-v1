package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.domain.shorts.ShortComment;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteShortCommentUseCaseTest {

    ShortCommentPort port;
    TransactionRunner transactionRunner;
    Clock clock;
    DeleteShortCommentUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(ShortCommentPort.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        clock = Clock.fixed(Instant.parse("2026-07-09T12:00:00Z"), ZoneOffset.UTC);
        useCase = new DeleteShortCommentUseCase(port, transactionRunner, clock);
    }

    private ShortComment ownedComment(Long ownerId, ShortComment parent) {
        User owner = User.create("owner@example.com").withId(ownerId);
        return new ShortComment(mock(ShortForm.class), owner, "내용", parent);
    }

    @Test
    void 최상위_댓글_삭제시_숏폼_댓글수만_감소하고_부모_reply_count는_감소하지_않는다() {
        when(port.findActiveComment(7721L, 15L)).thenReturn(Optional.of(ownedComment(1L, null)));

        useCase.execute(15L, 7721L, 1L);

        verify(port).updateContent(any(ShortComment.class));
        verify(port).adjustShortCommentCount(15L, -1);
        verify(port, never()).adjustParentReplyCount(any(), anyInt());
    }

    @Test
    void 대댓글_삭제시_부모_reply_count도_감소한다() {
        User owner = User.create("owner@example.com").withId(1L);
        ShortComment parent = new ShortComment(mock(ShortForm.class), owner, "부모 댓글", null);
        setId(parent, 7000L);
        ShortComment reply = ownedComment(1L, parent);
        when(port.findActiveComment(7721L, 15L)).thenReturn(Optional.of(reply));

        useCase.execute(15L, 7721L, 1L);

        verify(port).adjustShortCommentCount(15L, -1);
        verify(port).adjustParentReplyCount(7000L, -1);
    }

    @Test
    void 댓글이_없으면_COMMENT_NOT_FOUND_예외가_발생하고_카운터를_변경하지_않는다() {
        when(port.findActiveComment(999L, 15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(15L, 999L, 1L))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.COMMENT_NOT_FOUND);

        verify(port, never()).updateContent(any());
        verify(port, never()).adjustShortCommentCount(any(), anyInt());
    }

    @Test
    void 본인_댓글이_아니면_NOT_COMMENT_OWNER_예외가_발생하고_카운터를_변경하지_않는다() {
        when(port.findActiveComment(7721L, 15L)).thenReturn(Optional.of(ownedComment(2L, null)));

        assertThatThrownBy(() -> useCase.execute(15L, 7721L, 1L))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.NOT_COMMENT_OWNER);

        verify(port, never()).updateContent(any());
        verify(port, never()).adjustShortCommentCount(any(), anyInt());
    }

    private static void setId(ShortComment comment, Long id) {
        try {
            var field = ShortComment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(comment, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
