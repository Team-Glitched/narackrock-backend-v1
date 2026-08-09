package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentReactionPort;
import glitched.adlips.domain.reaction.ReactionType;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToggleShortCommentLikeUseCaseTest {

    ShortCommentReactionPort reactionPort;
    TransactionRunner transactionRunner;
    ToggleShortCommentLikeUseCase useCase;

    @BeforeEach
    void setUp() {
        reactionPort = mock(ShortCommentReactionPort.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
        useCase = new ToggleShortCommentLikeUseCase(reactionPort, transactionRunner);
        when(reactionPort.lockActiveComment(7721L, 15L)).thenReturn(true);
    }

    @Test
    void 좋아요가_없으면_LIKE를_저장하고_likeCount를_증가시킨다() {
        when(reactionPort.findReaction(1L, 7721L)).thenReturn(Optional.empty());
        when(reactionPort.getLikeCount(7721L)).thenReturn(5);

        ShortCommentLikeResult result = useCase.toggle(1L, 15L, 7721L);

        assertThat(result.isLiked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(5);
        verify(reactionPort).saveReaction(1L, 7721L, ReactionType.LIKE);
        verify(reactionPort).adjustLikeCount(7721L, 1);
        verify(reactionPort, never()).deleteReaction(any(), any());
        verify(reactionPort, never()).adjustDislikeCount(any(), anyInt());
    }

    @Test
    void 이미_좋아요_상태면_취소한다() {
        when(reactionPort.findReaction(1L, 7721L)).thenReturn(Optional.of(ReactionType.LIKE));
        when(reactionPort.getLikeCount(7721L)).thenReturn(4);

        ShortCommentLikeResult result = useCase.toggle(1L, 15L, 7721L);

        assertThat(result.isLiked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(4);
        verify(reactionPort).deleteReaction(1L, 7721L);
        verify(reactionPort).adjustLikeCount(7721L, -1);
        verify(reactionPort, never()).saveReaction(any(), any(), any());
    }

    @Test
    void 싫어요_상태면_좋아요로_전환한다() {
        when(reactionPort.findReaction(1L, 7721L)).thenReturn(Optional.of(ReactionType.DISLIKE));
        when(reactionPort.getLikeCount(7721L)).thenReturn(6);

        ShortCommentLikeResult result = useCase.toggle(1L, 15L, 7721L);

        assertThat(result.isLiked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(6);
        verify(reactionPort).saveReaction(1L, 7721L, ReactionType.LIKE);
        verify(reactionPort).adjustDislikeCount(7721L, -1);
        verify(reactionPort).adjustLikeCount(7721L, 1);
    }

    @Test
    void 댓글이_없으면_COMMENT_NOT_FOUND_예외가_발생하고_이후_단계는_호출되지_않는다() {
        when(reactionPort.lockActiveComment(999L, 15L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.toggle(1L, 15L, 999L))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.COMMENT_NOT_FOUND);

        verify(reactionPort, never()).findReaction(any(), any());
        verify(reactionPort, never()).saveReaction(any(), any(), any());
    }
}
