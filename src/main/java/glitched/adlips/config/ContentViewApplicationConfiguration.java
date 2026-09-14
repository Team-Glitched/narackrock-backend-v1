package glitched.adlips.config;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.view.RecordContentViewUseCase;
import glitched.adlips.application.view.SyncPendingContentViewsUseCase;
import glitched.adlips.application.view.port.out.ContentViewCounterPort;
import glitched.adlips.application.view.port.out.ContentViewExistencePort;
import glitched.adlips.application.view.port.out.ContentViewPendingPort;
import glitched.adlips.application.view.port.out.ContentViewPersistencePort;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ContentViewApplicationConfiguration {

    @Bean
    RecordContentViewUseCase recordContentViewUseCase(
            ContentViewExistencePort existencePort,
            ContentViewCounterPort counterPort,
            @Value("${app.redis.view-deduplication-window:PT30M}") Duration deduplicationWindow
    ) {
        return new RecordContentViewUseCase(existencePort, counterPort, deduplicationWindow);
    }

    @Bean
    SyncPendingContentViewsUseCase syncPendingContentViewsUseCase(
            ContentViewPendingPort pendingPort,
            ContentViewPersistencePort persistencePort,
            TransactionRunner transactionRunner,
            @Value("${app.redis.view-sync-batch-size:100}") int batchSize
    ) {
        return new SyncPendingContentViewsUseCase(
                pendingPort, persistencePort, transactionRunner, batchSize);
    }
}
