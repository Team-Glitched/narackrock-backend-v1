package glitched.adlips.application.notification;
import glitched.adlips.domain.notification.*;
import java.util.List;
public interface NotificationAudiencePort {
    record Delivery(Long recipientId,NotificationType type,String body,String path,String resourceType,Long resourceId,Long projectId,Long requestId,String key) {}
    record Pending(NotificationType type,Long actorId,Long targetId,Long referenceId) {}
    default List<Pending> pending(Long userId){return List.of();}
    List<Delivery> resolve(NotificationType type,Long actorId,Long targetId,Long referenceId,String detail);
    /** Refresh access and actionable request status when a saved notification is read. */
    record Access(boolean available,String requestStatus) {}
    Access access(Long userId,ServiceNotification notification);
}
