package glitched.adlips.domain.notification;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "service_notifications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"recipient_id", "event_key"}),
        indexes = {
            @Index(columnList = "recipient_id,id"),
            @Index(columnList = "recipient_id,read_at,id")
        })
public class ServiceNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private NotificationType type;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(length = 500)
    private String path;

    @Column(name = "resource_type", length = 40)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "event_key", nullable = false, length = 180)
    private String eventKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected ServiceNotification() {}

    public ServiceNotification(
            Long recipientId,
            NotificationType type,
            Long actorId,
            String title,
            String body,
            String path,
            String resourceType,
            Long resourceId,
            Long projectId,
            Long requestId,
            String eventKey,
            Instant createdAt) {
        this.recipientId = recipientId;
        this.type = type;
        this.actorId = actorId;
        this.title = title;
        this.body = body;
        this.path = path;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.projectId = projectId;
        this.requestId = requestId;
        this.eventKey = eventKey;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public NotificationType getType() {
        return type;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getPath() {
        return path;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public String getEventKey() {
        return eventKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
