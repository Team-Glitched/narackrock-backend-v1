package glitched.adlips.application.user.relation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.application.user.relation.port.out.FollowRepositoryPort;
import glitched.adlips.application.user.relation.usecase.DirectChatUseCase;
import org.junit.jupiter.api.Test;

class DirectChatUseCaseTest {
    private final FollowRepositoryPort follows = mock(FollowRepositoryPort.class);
    private final ChatMessagePort messages = mock(ChatMessagePort.class);
    private final DirectChatUseCase chat = new DirectChatUseCase(follows, messages);

    @Test
    void bothDirectionsMustFollowBeforeSending() {
        when(follows.exists(1L, 2L)).thenReturn(true);
        assertThrows(UserApplicationException.class, () -> chat.send(1L, 2L, "hello"));
        verifyNoInteractions(messages);
    }

    @Test
    void mutualFriendsCanSend() {
        when(follows.exists(1L, 2L)).thenReturn(true);
        when(follows.exists(2L, 1L)).thenReturn(true);
        chat.send(1L, 2L, "hello");
        verify(messages).save(1L, 2L, "hello");
    }
}
