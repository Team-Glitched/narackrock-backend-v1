package glitched.adlips.domain.user;

import glitched.adlips.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "direct_chat_messages", indexes = {
        @Index(columnList = "sender_id,recipient_id,id"),
        @Index(columnList = "recipient_id,sender_id,id"),
        @Index(name = "idx_direct_chat_recipient_unread", columnList = "recipient_id,is_unread,sender_id")
})
public class DirectChatMessage extends BaseCreatedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "is_unread", nullable = false, columnDefinition = "boolean default false")
    private boolean unread;
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    @Column(name = "revision", nullable = false, columnDefinition = "bigint default 0")
    private long revision;

    protected DirectChatMessage() {}

    public DirectChatMessage(Long senderId, Long recipientId, String content) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.unread = true;
    }

    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public Long getRecipientId() { return recipientId; }
    public String getContent() { return content; }
    public LocalDateTime getEditedAt() { return editedAt; }
    public boolean isDeleted() { return deleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public long getRevision() { return revision; }
}
