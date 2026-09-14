package glitched.adlips.adapter.out.scheduling;

import glitched.adlips.application.view.ContentViewSyncResult;
import glitched.adlips.application.view.SyncPendingContentViewsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.redis.view-sync-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ContentViewSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ContentViewSyncScheduler.class);

    private final SyncPendingContentViewsUseCase useCase;

    public ContentViewSyncScheduler(SyncPendingContentViewsUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(
            fixedDelayString = "${app.redis.view-sync-interval:PT10S}",
            initialDelayString = "${app.redis.view-sync-initial-delay:PT10S}"
    )
    public void synchronize() {
        ContentViewSyncResult result = useCase.execute();
        if (result.failedContentCount() > 0) {
            log.warn("조회수 동기화 일부 실패: success={}, views={}, failed={}",
                    result.syncedContentCount(),
                    result.syncedViewCount(),
                    result.failedContentCount());
        } else if (result.syncedContentCount() > 0) {
            log.debug("조회수 동기화 완료: contents={}, views={}",
                    result.syncedContentCount(), result.syncedViewCount());
        }
    }
}
