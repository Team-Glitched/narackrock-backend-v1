package glitched.adlips.application.view.port.out;

import glitched.adlips.application.view.ContentViewTarget;
import java.time.Duration;

public interface ContentViewCounterPort {

    boolean recordIfFirst(
            ContentViewTarget target,
            Long contentId,
            String viewerId,
            Duration deduplicationWindow
    );
}
