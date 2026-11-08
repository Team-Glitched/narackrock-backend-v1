package glitched.adlips.application.notification;
import glitched.adlips.domain.notification.NotificationType;
import java.time.Clock;
import java.util.Comparator;
import java.util.Objects;
public class NotificationActivityService implements NotificationEvents {
    private final NotificationStore store;
    private final NotificationAudiencePort audiences;
    private final NotificationDeliveryPort delivery;
    private final Clock clock;
    public NotificationActivityService(NotificationStore store,NotificationAudiencePort audiences,NotificationDeliveryPort delivery,Clock clock){
        this.store=store;this.audiences=audiences;this.delivery=delivery;this.clock=clock;
    }
    public void record(NotificationType type,Long actorId,Long targetId,Long referenceId,String detail){
        // All recipient locks use a stable order for multi-recipient fanout.
        for(var item:audiences.resolve(type,actorId,targetId,referenceId,detail).stream()
                .sorted(Comparator.comparing(NotificationAudiencePort.Delivery::recipientId)).toList()){
            if(item.recipientId()==null||Objects.equals(item.recipientId(),actorId)||!store.preferences(item.recipientId()).allows(item.type()))continue;
            String key=item.key()==null?item.type()+":"+actorId+":"+targetId+":"+referenceId:item.key();
            if(store.insert(new NotificationDraft(item.recipientId(),item.type(),actorId,item.type().title,item.body(),
                    item.path(),item.resourceType(),item.resourceId(),item.projectId(),item.requestId(),key,clock.instant())))delivery.changed(item.recipientId());
        }
        if(actorId!=null&&(type==NotificationType.FOLLOW_ACCEPTED||type==NotificationType.FOLLOW_REJECTED
                ||type==NotificationType.CONTRIBUTION_ACCEPTED||type==NotificationType.CONTRIBUTION_REJECTED))delivery.changed(actorId);
    }
}
