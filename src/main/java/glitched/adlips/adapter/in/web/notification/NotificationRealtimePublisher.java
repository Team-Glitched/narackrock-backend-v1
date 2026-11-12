package glitched.adlips.adapter.in.web.notification;

import glitched.adlips.application.notification.NotificationDeliveryPort;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

import java.util.Map;

@Component
public class NotificationRealtimePublisher implements NotificationDeliveryPort {
    private final SimpMessagingTemplate messages;

    public NotificationRealtimePublisher(SimpMessagingTemplate messages) {
        this.messages = messages;
    }

    public void changed(Long user) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    public void afterCommit() {
                        try {
                            messages.convertAndSendToUser(
                                    user.toString(),
                                    "/queue/notifications",
                                    Map.of("changed", true));
                        } catch (RuntimeException ignored) {
                            /* Durable inbox is reloaded on reconnect. */
                        }
                    }
                });
    }
}
