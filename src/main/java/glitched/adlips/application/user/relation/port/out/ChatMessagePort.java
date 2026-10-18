package glitched.adlips.application.user.relation.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessagePort {
    record Message(Long id, Long senderId, Long recipientId, String content, LocalDateTime createdAt) {}
    Message save(Long senderId, Long recipientId, String content);
    List<Message> conversation(Long firstId, Long secondId, Long beforeId, int size);
    List<Message> latestConversations(Long userId, int limit);
}
