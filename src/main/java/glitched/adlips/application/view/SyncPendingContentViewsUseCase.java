package glitched.adlips.application.view;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.view.port.out.ContentViewPendingPort;
import glitched.adlips.application.view.port.out.ContentViewPersistencePort;
import java.util.Objects;

public class SyncPendingContentViewsUseCase {

    private final ContentViewPendingPort pendingPort;
    private final ContentViewPersistencePort persistencePort;
    private final TransactionRunner transactionRunner;
    private final int batchSize;

    public SyncPendingContentViewsUseCase(
            ContentViewPendingPort pendingPort,
            ContentViewPersistencePort persistencePort,
            TransactionRunner transactionRunner,
            int batchSize
    ) {
        this.pendingPort = Objects.requireNonNull(pendingPort);
        this.persistencePort = Objects.requireNonNull(persistencePort);
        this.transactionRunner = Objects.requireNonNull(transactionRunner);
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        this.batchSize = batchSize;
    }

    public ContentViewSyncResult execute() {
        int syncedContentCount = 0;
        long syncedViewCount = 0;
        int failedContentCount = 0;

        for (ContentViewKey key : pendingPort.findPending(batchSize)) {
            ClaimedContentViews claimed = pendingPort.claim(key);
            if (claimed.isEmpty()) {
                continue;
            }

            try {
                transactionRunner.required(() -> {
                    persistencePort.increment(key, claimed.batchId(), claimed.count());
                    return null;
                });
                pendingPort.complete(key, claimed.batchId(), claimed.count());
                syncedContentCount++;
                syncedViewCount += claimed.count();
            } catch (RuntimeException exception) {
                pendingPort.restore(key, claimed.batchId(), claimed.count());
                failedContentCount++;
            }
        }

        return new ContentViewSyncResult(
                syncedContentCount, syncedViewCount, failedContentCount);
    }
}
