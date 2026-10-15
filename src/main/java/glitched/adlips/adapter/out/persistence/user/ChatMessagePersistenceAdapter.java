package glitched.adlips.adapter.out.persistence.user;

import glitched.adlips.application.user.relation.port.out.ChatMessagePort;
import glitched.adlips.domain.user.DirectChatMessage;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

interface SpringDataDirectChatMessageRepository extends JpaRepository<DirectChatMessage, Long> {
    @Query("select m from DirectChatMessage m where ((m.senderId = :a and m.recipientId = :b) or (m.senderId = :b and m.recipientId = :a)) and (:beforeId is null or m.id < :beforeId) order by m.id desc")
    List<DirectChatMessage> conversation(@Param("a") Long a, @Param("b") Long b, @Param("beforeId") Long beforeId, org.springframework.data.domain.Pageable pageable);
}

@Repository
public class ChatMessagePersistenceAdapter implements ChatMessagePort {
    private final SpringDataDirectChatMessageRepository repository;
    public ChatMessagePersistenceAdapter(SpringDataDirectChatMessageRepository repository) { this.repository = repository; }
    public Message save(Long senderId, Long recipientId, String content) { return map(repository.save(new DirectChatMessage(senderId, recipientId, content))); }
    public List<Message> conversation(Long firstId, Long secondId, Long beforeId, int size) {
        var descending = repository.conversation(firstId, secondId, beforeId, PageRequest.of(0, size));
        return descending.reversed().stream().map(this::map).toList();
    }
    private Message map(DirectChatMessage message) { return new Message(message.getId(), message.getSenderId(), message.getRecipientId(), message.getContent(), message.getCreatedAt()); }
}
