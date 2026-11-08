package glitched.adlips.application.notification;

import glitched.adlips.domain.notification.NotificationType;

/** Records a successful activity in the same transaction as the change. */
@FunctionalInterface
public interface NotificationEvents {
    void record(
            NotificationType type, Long actorId, Long targetId, Long referenceId, String detail);

    default void record(NotificationType type, Long actorId, Long targetId) {
        record(type, actorId, targetId, null, null);
    }

    static NotificationEvents none() {
        return (type, actor, target, reference, detail) -> {};
    }
}
