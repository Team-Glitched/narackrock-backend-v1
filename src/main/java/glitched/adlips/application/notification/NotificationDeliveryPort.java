package glitched.adlips.application.notification;
@FunctionalInterface
public interface NotificationDeliveryPort {
    void changed(Long recipientId);
}
