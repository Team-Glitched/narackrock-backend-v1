package glitched.adlips.adapter.in.web.user;

import java.security.Principal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ChatStompAuthenticationInterceptor implements ChannelInterceptor {
    private final AuthenticatedUserResolver users;

    public ChatStompAuthenticationInterceptor(AuthenticatedUserResolver users) {
        this.users = users;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null || headers.getCommand() == null) return message;

        if (headers.getCommand() == StompCommand.CONNECT) {
            Long userId = users.requireUserId(headers.getFirstNativeHeader("Authorization"));
            headers.setUser(new ChatPrincipal(userId.toString()));
        } else if (headers.getCommand() == StompCommand.SUBSCRIBE) {
            if (headers.getUser() == null || !("/user/queue/chat".equals(headers.getDestination()) || "/user/queue/notifications".equals(headers.getDestination()))) {
                throw new AccessDeniedException("채팅 구독 권한이 없습니다.");
            }
        } else if (headers.getCommand() == StompCommand.SEND) {
            throw new AccessDeniedException("채팅 전송은 인증된 REST API를 사용해 주세요.");
        }
        return message;
    }

    private record ChatPrincipal(String name) implements Principal {
        @Override
        public String getName() { return name; }
    }
}
