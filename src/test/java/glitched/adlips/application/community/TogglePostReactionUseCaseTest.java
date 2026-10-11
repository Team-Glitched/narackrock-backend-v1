package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostReactionPort;
import glitched.adlips.application.community.port.out.ReactionCounts;
import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.reaction.ReactionType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TogglePostReactionUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T00:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock PostReactionPort reactionPort;
    @Mock UserBanQueryPort userBanQueryPort;

    TogglePostReactionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TogglePostReactionUseCase(reactionPort, userBanQueryPort, TransactionRunner.direct(), CLOCK);
    }

    @Test
    void 반응이_없으면_좋아요를_추가하고_갱신된_수를_반환한다() {
        when(reactionPort.lockActivePost(10L)).thenReturn(true);
        when(reactionPort.findReaction(1L, 10L)).thenReturn(Optional.empty());
        when(reactionPort.findReactionCounts(10L)).thenReturn(new ReactionCounts(5, 2));

        PostReactionResult result = useCase.toggle(1L, 10L, ReactionType.LIKE);

        verify(reactionPort).saveReaction(1L, 10L, ReactionType.LIKE);
        verify(reactionPort).adjustReactionCounts(10L, 1, 0);
        assertThat(result).isEqualTo(new PostReactionResult(10L, true, false, 5, 2));
    }

    @Test
    void 같은_반응을_다시_누르면_반응을_삭제한다() {
        when(reactionPort.lockActivePost(10L)).thenReturn(true);
        when(reactionPort.findReaction(1L, 10L)).thenReturn(Optional.of(ReactionType.DISLIKE));
        when(reactionPort.findReactionCounts(10L)).thenReturn(new ReactionCounts(5, 1));

        PostReactionResult result = useCase.toggle(1L, 10L, ReactionType.DISLIKE);

        verify(reactionPort).deleteReaction(1L, 10L);
        verify(reactionPort, never()).saveReaction(any(), any(), any());
        verify(reactionPort).adjustReactionCounts(10L, 0, -1);
        assertThat(result).isEqualTo(new PostReactionResult(10L, false, false, 5, 1));
    }

    @Test
    void 반대_반응을_누르면_전환한다() {
        when(reactionPort.lockActivePost(10L)).thenReturn(true);
        when(reactionPort.findReaction(1L, 10L)).thenReturn(Optional.of(ReactionType.LIKE));
        when(reactionPort.findReactionCounts(10L)).thenReturn(new ReactionCounts(4, 3));

        PostReactionResult result = useCase.toggle(1L, 10L, ReactionType.DISLIKE);

        verify(reactionPort).saveReaction(1L, 10L, ReactionType.DISLIKE);
        verify(reactionPort).adjustReactionCounts(10L, -1, 1);
        assertThat(result).isEqualTo(new PostReactionResult(10L, false, true, 4, 3));
    }

    @Test
    void 없거나_삭제된_게시글이면_POST_NOT_FOUND() {
        when(reactionPort.lockActivePost(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.toggle(1L, 999L, ReactionType.LIKE))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode").isEqualTo(PostErrorCode.POST_NOT_FOUND);
        verify(reactionPort, never()).adjustReactionCounts(any(), anyInt(), anyInt());
    }

    @Test
    void 이용_정지_사용자면_BANNED_USER_ACCESS() {
        when(userBanQueryPort.isBanned(1L, NOW)).thenReturn(true);

        assertThatThrownBy(() -> useCase.toggle(1L, 10L, ReactionType.LIKE))
                .isInstanceOf(PostApplicationException.class)
                .extracting("errorCode").isEqualTo(PostErrorCode.BANNED_USER_ACCESS);
        verify(reactionPort, never()).lockActivePost(any());
    }
}
