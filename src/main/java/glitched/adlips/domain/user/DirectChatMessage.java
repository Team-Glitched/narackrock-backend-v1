package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "direct_chat_messages", indexes = {@Index(columnList = "sender_id,recipient_id,id"), @Index(columnList = "recipient_id,sender_id,id")})
public class DirectChatMessage extends BaseCreatedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;
    @Column(nullable = false, length = 2000)
    private String content;
    protected DirectChatMessage() {}
    public DirectChatMessage(Long senderId, Long recipientId, String content) { this.senderId = senderId; this.recipientId = recipientId; this.content = content; }
    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public Long getRecipientId() { return recipientId; }
    public String getContent() { return content; }
}
