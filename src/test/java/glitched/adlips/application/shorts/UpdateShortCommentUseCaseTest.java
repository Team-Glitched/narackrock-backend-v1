package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentPort;
import glitched.adlips.domain.shorts.ShortComment;
import glitched.adlips.domain.shorts.ShortForm;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateShortCommentUseCaseTest {

    ShortCommentPort port;
    TransactionRunner transactionRunner;
    UpdateShortCommentUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(ShortCommentPort.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new UpdateShortCommentUseCase(port, transactionRunner);
    }

    private ShortComment ownedComment(Long ownerId) {
        User owner = User.create("owner@example.com").withId(ownerId);
        return new ShortComment(mock(ShortForm.class), owner, "원래 내용", null);
    }

    @Test
    void 정상_수정하면_commentId를_반환하고_포트에_반영한다() {
        when(port.findActiveComment(7721L, 15L)).thenReturn(Optional.of(ownedComment(1L)));

        Long result = useCase.execute(15L, 7721L, 1L, "수정된 댓글 내용입니다.");

        assertThat(result).isEqualTo(7721L);
        verify(port).updateContent(any(ShortComment.class));
    }

    @Test
    void content가_공백이면_INVALID_INPUT_VALUE_예외가_발생하고_조회하지_않는다() {
        assertThatThrownBy(() -> useCase.execute(15L, 7721L, 1L, "  "))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.INVALID_INPUT_VALUE);

        verify(port, never()).findActiveComment(any(), any());
    }

    @Test
    void 댓글이_없으면_COMMENT_NOT_FOUND_예외가_발생하고_수정하지_않는다() {
        when(port.findActiveComment(999L, 15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(15L, 999L, 1L, "내용"))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.COMMENT_NOT_FOUND);

        verify(port, never()).updateContent(any());
    }

    @Test
    void 본인_댓글이_아니면_NOT_COMMENT_OWNER_예외가_발생하고_수정하지_않는다() {
        when(port.findActiveComment(7721L, 15L)).thenReturn(Optional.of(ownedComment(2L)));

        assertThatThrownBy(() -> useCase.execute(15L, 7721L, 1L, "내용"))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.NOT_COMMENT_OWNER);

        verify(port, never()).updateContent(any());
    }
}
