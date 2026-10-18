package glitched.adlips.adapter.in.web.user;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class ChatRealtimePublisherTest {
    @Test
    void savedMessageIsDeliveredToBothParticipants() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        ChatMessagePort.Message saved = new ChatMessagePort.Message(10L, 1L, 2L, "안녕하세요", LocalDateTime.now());

        new ChatRealtimePublisher(messaging).publish(saved);

        verify(messaging).convertAndSendToUser("1", "/queue/chat", saved);
        verify(messaging).convertAndSendToUser("2", "/queue/chat", saved);
    }
}
