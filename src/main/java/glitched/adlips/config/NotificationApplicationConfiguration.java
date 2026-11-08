package glitched.adlips.config;
import glitched.adlips.application.notification.*;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import org.springframework.context.annotation.*;
@Configuration
public class NotificationApplicationConfiguration {
    @Bean NotificationEvents notificationEvents(NotificationStore store,NotificationAudiencePort audiences,NotificationDeliveryPort delivery,Clock clock){return new NotificationActivityService(store,audiences,delivery,clock);}
    @Bean NotificationInboxUseCase notificationInboxUseCase(NotificationStore store,NotificationAudiencePort audiences,TransactionRunner transactions,NotificationDeliveryPort delivery,Clock clock,NotificationEvents events){return new NotificationInboxUseCase(store,audiences,transactions,delivery,clock,events);}
}
