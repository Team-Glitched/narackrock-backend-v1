package glitched.adlips.adapter.in.web.user;

import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatRealtimePublisher {
    private final SimpMessagingTemplate messaging;

    public ChatRealtimePublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void publish(ChatMessagePort.Message message) {
        messaging.convertAndSendToUser(message.senderId().toString(), "/queue/chat", message);
        messaging.convertAndSendToUser(message.recipientId().toString(), "/queue/chat", message);
    }
}
