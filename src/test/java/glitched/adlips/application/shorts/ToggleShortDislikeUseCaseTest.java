package glitched.adlips.application.shorts;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortReactionPort;
import glitched.adlips.domain.reaction.ReactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToggleShortDislikeUseCaseTest {

    @Mock ShortReactionPort reactionPort;
    @Mock TransactionRunner transactionRunner;

    ToggleShortDislikeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ToggleShortDislikeUseCase(reactionPort, transactionRunner);
        when(transactionRunner.required(any())).thenAnswer(inv ->
                inv.<Supplier<?>>getArgument(0).get());
    }

    @Test
    void 반응_없음_싫어요_추가() {
        when(reactionPort.findReaction(1L, 12L)).thenReturn(Optional.empty());
        when(reactionPort.getDislikeCount(12L)).thenReturn(129);

        ShortDislikeResult result = useCase.toggle(1L, 12L);

        verify(reactionPort).saveReaction(1L, 12L, ReactionType.DISLIKE);
        verify(reactionPort).adjustDislikeCount(12L, +1);
        assertThat(result.isDisliked()).isTrue();
        assertThat(result.dislikeCount()).isEqualTo(129);
        assertThat(result.shortId()).isEqualTo(12L);
    }

    @Test
    void 이미_싫어요_상태_싫어요_취소() {
        when(reactionPort.findReaction(1L, 12L)).thenReturn(Optional.of(ReactionType.DISLIKE));
        when(reactionPort.getDislikeCount(12L)).thenReturn(128);

        ShortDislikeResult result = useCase.toggle(1L, 12L);

        verify(reactionPort).deleteReaction(1L, 12L);
        verify(reactionPort).adjustDislikeCount(12L, -1);
        verify(reactionPort, never()).saveReaction(any(), any(), any());
        assertThat(result.isDisliked()).isFalse();
        assertThat(result.dislikeCount()).isEqualTo(128);
    }

    @Test
    void 좋아요_상태_싫어요로_전환() {
        when(reactionPort.findReaction(1L, 12L)).thenReturn(Optional.of(ReactionType.LIKE));
        when(reactionPort.getDislikeCount(12L)).thenReturn(130);

        ShortDislikeResult result = useCase.toggle(1L, 12L);

        verify(reactionPort).saveReaction(1L, 12L, ReactionType.DISLIKE);
        verify(reactionPort).adjustLikeCount(12L, -1);
        verify(reactionPort).adjustDislikeCount(12L, +1);
        verify(reactionPort, never()).deleteReaction(any(), any());
        assertThat(result.isDisliked()).isTrue();
    }
}
