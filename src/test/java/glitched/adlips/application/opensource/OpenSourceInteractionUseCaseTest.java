package glitched.adlips.application.opensource;

import glitched.adlips.application.community.port.out.UserBanQueryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.reaction.ReactionType;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static glitched.adlips.application.opensource.OpenSourceInteractionPort.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenSourceInteractionUseCaseTest {
    final OpenSourceInteractionPort port=mock(OpenSourceInteractionPort.class);
    final UserBanQueryPort bans=mock(UserBanQueryPort.class);
    final OpenSourceInteractionUseCase useCase=new OpenSourceInteractionUseCase(port,TransactionRunner.direct(),bans,Clock.systemUTC());
    @Test void bannedUserCannotCreateOrReact() {
        when(bans.isBanned(eq(1L),any())).thenReturn(true);
        assertThatThrownBy(()->useCase.create(1L,10L,"comment",null)).isInstanceOf(OpenSourceException.class)
                .satisfies(e->assertThat(((OpenSourceException)e).getCode()).isEqualTo(OpenSourceException.Code.OPEN_SOURCE_ACCESS_DENIED));
        assertThatThrownBy(()->useCase.react(1L,10L,Target.SOURCE,ReactionType.LIKE)).isInstanceOf(OpenSourceException.class);
        verifyNoInteractions(port);
    }
    @Test void reactionLocksSourceBeforeReadingAndChangingState() {
        when(port.lockAvailableSource(10L)).thenReturn(true);
        when(port.reaction(1L,Target.SOURCE,10L)).thenReturn(Optional.of(ReactionType.LIKE));
        useCase.react(1L,10L,Target.SOURCE,ReactionType.LIKE);
        var order=inOrder(port);
        order.verify(port).lockAvailableSource(10L);
        order.verify(port).reaction(1L,Target.SOURCE,10L);
        order.verify(port).setReaction(eq(1L),eq(Target.SOURCE),eq(10L),isNull(),any());
    }
    @Test void hiddenSourceDoesNotInsertComment() {
        assertThatThrownBy(()->useCase.create(1L,10L,"comment",null)).isInstanceOf(OpenSourceException.class);
        verify(port,never()).insertComment(any(),any(),any(),any(),any());
    }
    @Test void crossSourceParentAndReplyParentAreDifferentErrors() {
        when(port.lockAvailableSource(10L)).thenReturn(true);
        when(port.findComment(20L)).thenReturn(Optional.of(new CommentRef(20L,11L,null)));
        assertThatThrownBy(()->useCase.create(1L,10L,"comment",20L)).isInstanceOf(OpenSourceException.class)
                .satisfies(e->assertThat(((OpenSourceException)e).getCode()).isEqualTo(OpenSourceException.Code.OPEN_SOURCE_COMMENT_NOT_FOUND));
        when(port.findComment(20L)).thenReturn(Optional.of(new CommentRef(20L,10L,19L)));
        assertThatThrownBy(()->useCase.create(1L,10L,"comment",20L)).isInstanceOf(OpenSourceException.class)
                .satisfies(e->assertThat(((OpenSourceException)e).getCode()).isEqualTo(OpenSourceException.Code.INVALID_REQUEST));
    }
}
