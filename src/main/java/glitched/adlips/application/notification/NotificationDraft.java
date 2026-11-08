package glitched.adlips.application.notification;

import glitched.adlips.domain.notification.*;

import java.time.Instant;

public record NotificationDraft(
        long recipientId,
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
    public ServiceNotification entity() {
        return new ServiceNotification(
                recipientId,
                type,
                actorId,
                title,
                body,
                path,
                resourceType,
                resourceId,
                projectId,
                requestId,
                eventKey,
                createdAt);
    }
}
