package glitched.adlips.application.community;

import glitched.adlips.application.community.port.out.GalleryAuthorityPort;
import glitched.adlips.application.community.port.out.GalleryModerationLogItem;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.application.port.TransactionRunner;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class GetGalleryModerationLogsUseCase {

    private final GalleryAuthorityPort authorityPort;
    private final GalleryModerationLogPort logPort;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public GetGalleryModerationLogsUseCase(
            GalleryAuthorityPort authorityPort,
            GalleryModerationLogPort logPort,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.authorityPort = authorityPort;
        this.logPort = logPort;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public GalleryModerationLogPage execute(Long actorId, Long galleryId, Long cursor, int size) {
        return transactionRunner.readOnly(() -> {
            GalleryModerationGuard.requireGallery(authorityPort, galleryId);
            GalleryModerationGuard.requireManager(authorityPort, actorId, galleryId, LocalDateTime.now(clock));
            GalleryModerationGuard.validatePage(cursor, size);

            List<GalleryModerationLogItem> queried = logPort.findPage(galleryId, cursor, size + 1);
            boolean hasNext = queried.size() > size;
            List<GalleryModerationLogItem> content = hasNext ? queried.subList(0, size) : queried;
            return new GalleryModerationLogPage(
                    List.copyOf(content), hasNext ? content.getLast().logId() : null, hasNext);
        });
    }
}
