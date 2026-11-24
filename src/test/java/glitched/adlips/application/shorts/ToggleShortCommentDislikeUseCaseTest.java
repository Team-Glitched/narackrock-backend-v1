package glitched.adlips.application.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.shorts.port.out.ShortCommentReactionPort;
import glitched.adlips.application.shorts.port.out.UserBanQueryPort;
import glitched.adlips.domain.reaction.ReactionType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToggleShortCommentDislikeUseCaseTest {
    ShortCommentReactionPort port;
    TransactionRunner transactionRunner;
    ToggleShortCommentDislikeUseCase useCase;

    @BeforeEach
    void setUp() {
        port = mock(ShortCommentReactionPort.class);
        transactionRunner = mock(TransactionRunner.class);
        when(transactionRunner.required(any())).thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
        when(port.lockActiveComment(10L, 20L)).thenReturn(true);
        useCase = new ToggleShortCommentDislikeUseCase(port, transactionRunner);
    }

    @Test
    void 처음_싫어요를_누르면_카운트가_증가한다() {
        when(port.findReaction(1L, 10L)).thenReturn(Optional.empty());
        when(port.getLikeCount(10L)).thenReturn(2);
        when(port.getDislikeCount(10L)).thenReturn(1);

        ShortCommentDislikeResult result = useCase.toggle(1L, 20L, 10L);

        assertThat(result.isLiked()).isFalse();
        assertThat(result.isDisliked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(2);
        assertThat(result.dislikeCount()).isEqualTo(1);
        verify(port).saveReaction(1L, 10L, ReactionType.DISLIKE);
        verify(port).adjustDislikeCount(10L, 1);
    }

    @Test
    void 같은_싫어요를_다시_누르면_취소한다() {
        when(port.findReaction(1L, 10L)).thenReturn(Optional.of(ReactionType.DISLIKE));
        when(port.getLikeCount(10L)).thenReturn(3);
        when(port.getDislikeCount(10L)).thenReturn(0);

        ShortCommentDislikeResult result = useCase.toggle(1L, 20L, 10L);

        assertThat(result.isLiked()).isFalse();
        assertThat(result.isDisliked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(3);
        assertThat(result.dislikeCount()).isEqualTo(0);
        verify(port).deleteReaction(1L, 10L);
        verify(port).adjustDislikeCount(10L, -1);
    }

    @Test
    void 좋아요에서_싫어요로_전환하면_두_카운트가_변한다() {
        when(port.findReaction(1L, 10L)).thenReturn(Optional.of(ReactionType.LIKE));
        when(port.getLikeCount(10L)).thenReturn(1);
        when(port.getDislikeCount(10L)).thenReturn(4);

        ShortCommentDislikeResult result = useCase.toggle(1L, 20L, 10L);

        assertThat(result.isLiked()).isFalse();
        assertThat(result.isDisliked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1);
        assertThat(result.dislikeCount()).isEqualTo(4);
        verify(port).saveReaction(1L, 10L, ReactionType.DISLIKE);
        verify(port).adjustLikeCount(10L, -1);
        verify(port).adjustDislikeCount(10L, 1);
    }

    @Test
    void 정지된_사용자는_싫어요를_토글할_수_없다() {
        UserBanQueryPort banPort = mock(UserBanQueryPort.class);
        Clock fixed = Clock.fixed(Instant.parse("2026-11-24T00:00:00Z"), ZoneOffset.UTC);
        when(banPort.isBanned(any(), any())).thenReturn(true);
        var restricted = new ToggleShortCommentDislikeUseCase(port, transactionRunner, banPort, fixed);

        assertThatThrownBy(() -> restricted.toggle(1L, 20L, 10L))
                .isInstanceOf(ShortCommentApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ShortCommentErrorCode.BANNED_USER_ACCESS);
        verify(port, never()).lockActiveComment(any(), any());
    }
}
