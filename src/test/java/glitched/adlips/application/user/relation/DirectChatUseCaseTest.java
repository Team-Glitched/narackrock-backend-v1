package glitched.adlips.application.user.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.usecase.DirectChatUseCase;
import glitched.adlips.domain.user.Profile;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DirectChatUseCaseTest {
    private final FollowRepositoryPort follows = mock(FollowRepositoryPort.class);
    private final ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
    private final ChatMessagePort messages = mock(ChatMessagePort.class);
    private final DirectChatUseCase chat = new DirectChatUseCase(follows, profiles, messages);

    private void privacy(boolean senderPrivate, boolean recipientPrivate) {
        Profile sender = mock(Profile.class);
        Profile recipient = mock(Profile.class);
        when(sender.isPrivate()).thenReturn(senderPrivate);
        when(recipient.isPrivate()).thenReturn(recipientPrivate);
        when(profiles.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(profiles.findByUserId(2L)).thenReturn(Optional.of(recipient));
    }

    @Test
    void privateRecipientRequiresMutualFollowAndReturnsForbidden() {
        privacy(false, true);
        when(follows.exists(1L, 2L)).thenReturn(true);
        UserApplicationException error = assertThrows(UserApplicationException.class, () -> chat.send(1L, 2L, "hello"));
        assertEquals(UserErrorCode.CHAT_FORBIDDEN, error.getErrorCode());
        verifyNoInteractions(messages);
    }

    @Test
    void publicRecipientCanReceiveWithoutFollow() {
        privacy(false, false);
        chat.send(1L, 2L, "hello");
        verify(messages).save(1L, 2L, "hello");
        chat.conversation(1L, 2L, null, 50);
        verify(messages).conversation(1L, 2L, null, 50);
    }

    @Test
    void privateSenderCanMessagePublicRecipientWithoutFollow() {
        privacy(true, false);
        chat.send(1L, 2L, "hello");
        verify(messages).save(1L, 2L, "hello");
    }

    @Test
    void publicRecipientCanReadMessagesFromPrivateSender() {
        privacy(true, false);
        chat.conversation(2L, 1L, null, 50);
        verify(messages).conversation(2L, 1L, null, 50);
    }

    @Test
    void privateRecipientCanChatWhenBothDirectionsFollow() {
        privacy(false, true);
        when(follows.exists(1L, 2L)).thenReturn(true);
        when(follows.exists(2L, 1L)).thenReturn(true);
        chat.send(1L, 2L, "hello");
        verify(messages).save(1L, 2L, "hello");
    }

    @Test
    void privateConversationCannotBeReadAfterUnfollow() {
        privacy(true, true);
        when(follows.exists(1L, 2L)).thenReturn(true);

        UserApplicationException error = assertThrows(UserApplicationException.class,
                () -> chat.conversation(1L, 2L, null, 50));

        assertEquals(UserErrorCode.CHAT_FORBIDDEN, error.getErrorCode());
        verifyNoInteractions(messages);
    }

    @Test
    void privateSenderCannotMessagePrivateRecipientWithoutMutualFollow() {
        privacy(true, true);
        UserApplicationException error = assertThrows(UserApplicationException.class,
                () -> chat.send(1L, 2L, "hello"));
        assertEquals(UserErrorCode.CHAT_FORBIDDEN, error.getErrorCode());
        verifyNoInteractions(messages);
    }
}
