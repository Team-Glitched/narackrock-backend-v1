package glitched.adlips.application.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import glitched.adlips.application.community.port.out.PostCommentReactionPort;
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
class TogglePostCommentReactionUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T00:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock PostCommentReactionPort reactionPort;
    @Mock UserBanQueryPort userBanQueryPort;

    TogglePostCommentReactionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TogglePostCommentReactionUseCase(
                reactionPort, userBanQueryPort, TransactionRunner.direct(), CLOCK);
    }

    @Test
    void 반응이_없으면_좋아요를_추가한다() {
        when(reactionPort.lockActiveComment(30L)).thenReturn(true);
        when(reactionPort.findReaction(1L, 30L)).thenReturn(Optional.empty());
        when(reactionPort.findReactionCounts(30L)).thenReturn(new ReactionCounts(1, 0));

        PostCommentReactionResult result = useCase.toggle(1L, 30L, ReactionType.LIKE);

        verify(reactionPort).saveReaction(1L, 30L, ReactionType.LIKE);
        verify(reactionPort).adjustReactionCounts(30L, 1, 0);
        assertThat(result).isEqualTo(new PostCommentReactionResult(30L, true, false, 1, 0));
    }

    @Test
    void 같은_반응을_다시_누르면_취소한다() {
        when(reactionPort.lockActiveComment(30L)).thenReturn(true);
        when(reactionPort.findReaction(1L, 30L)).thenReturn(Optional.of(ReactionType.LIKE));
        when(reactionPort.findReactionCounts(30L)).thenReturn(new ReactionCounts(0, 0));

        PostCommentReactionResult result = useCase.toggle(1L, 30L, ReactionType.LIKE);

        verify(reactionPort).deleteReaction(1L, 30L);
        verify(reactionPort).adjustReactionCounts(30L, -1, 0);
        assertThat(result).isEqualTo(new PostCommentReactionResult(30L, false, false, 0, 0));
    }

    @Test
    void 반대_반응을_누르면_전환한다() {
        when(reactionPort.lockActiveComment(30L)).thenReturn(true);
        when(reactionPort.findReaction(1L, 30L)).thenReturn(Optional.of(ReactionType.LIKE));
        when(reactionPort.findReactionCounts(30L)).thenReturn(new ReactionCounts(0, 1));

        PostCommentReactionResult result = useCase.toggle(1L, 30L, ReactionType.DISLIKE);

        verify(reactionPort).saveReaction(1L, 30L, ReactionType.DISLIKE);
        verify(reactionPort).adjustReactionCounts(30L, -1, 1);
        assertThat(result).isEqualTo(new PostCommentReactionResult(30L, false, true, 0, 1));
    }

    @Test
    void 없거나_삭제된_댓글이면_COMMENT_NOT_FOUND() {
        when(reactionPort.lockActiveComment(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.toggle(1L, 999L, ReactionType.LIKE))
                .isInstanceOf(PostCommentApplicationException.class)
                .extracting("errorCode").isEqualTo(PostCommentErrorCode.COMMENT_NOT_FOUND);
        verify(reactionPort, never()).adjustReactionCounts(any(), anyInt(), anyInt());
    }

    @Test
    void 이용_정지_사용자면_BANNED_USER_ACCESS() {
        when(userBanQueryPort.isBanned(1L, NOW)).thenReturn(true);

        assertThatThrownBy(() -> useCase.toggle(1L, 30L, ReactionType.LIKE))
                .isInstanceOf(PostCommentApplicationException.class)
                .extracting("errorCode").isEqualTo(PostCommentErrorCode.BANNED_USER_ACCESS);
        verify(reactionPort, never()).lockActiveComment(any());
    }
}
