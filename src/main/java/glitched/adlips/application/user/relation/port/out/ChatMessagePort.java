package glitched.adlips.application.user.relation.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessagePort {
    /** {@code revision} increases with every committed edit or deletion; clients order updates by it. */
    record Message(Long id, Long senderId, Long recipientId, String content, LocalDateTime createdAt,
                   LocalDateTime editedAt, boolean deleted, long revision) {
        public Message(Long id, Long senderId, Long recipientId, String content, LocalDateTime createdAt) {
            this(id, senderId, recipientId, content, createdAt, null, false, 0L);
        }
    }
    record UnreadBySender(Long senderId, long count) {}

    Message save(Long senderId, Long recipientId, String content);
    Optional<Message> findById(Long messageId);
    Optional<Message> updateContent(Long messageId, String content, LocalDateTime editedAt);
    Message softDelete(Long messageId, LocalDateTime deletedAt);
    List<Message> conversation(Long firstId, Long secondId, Long beforeId, int size);
    List<Message> latestConversations(Long userId, int limit);
    List<UnreadBySender> unreadBySender(Long recipientId);
    void markRead(Long recipientId, Long senderId, Long throughId);
}
