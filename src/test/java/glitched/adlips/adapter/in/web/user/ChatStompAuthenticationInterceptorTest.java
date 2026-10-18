package glitched.adlips.adapter.in.web.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class ChatStompAuthenticationInterceptorTest {
    private final AuthenticatedUserResolver users = mock(AuthenticatedUserResolver.class);
    private final ChatStompAuthenticationInterceptor interceptor = new ChatStompAuthenticationInterceptor(users);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void connectRequiresValidAccessTokenAndSetsUser() {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.addNativeHeader("Authorization", "Bearer token");
        when(users.requireUserId("Bearer token")).thenReturn(7L);

        interceptor.preSend(message(headers), channel);

        assertNotNull(headers.getUser());
        assertEquals("7", headers.getUser().getName());
    }

    @Test
    void subscribeOnlyAllowsOwnChatQueue() {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setUser(() -> "7");
        headers.setDestination("/user/queue/chat");
        interceptor.preSend(message(headers), channel);

        headers.setDestination("/queue/chat");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(headers), channel));
    }

    @Test
    void nonMutualPrivateChatUsesForbiddenHttpStatus() {
        var handler = new UserExceptionHandler();
        var response = handler.handle(new UserApplicationException(UserErrorCode.CHAT_FORBIDDEN, "채팅 불가"));
        assertEquals(403, response.getStatusCode().value());
    }

    private Message<byte[]> message(StompHeaderAccessor headers) {
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
