package glitched.adlips.application.view.port.out;

import glitched.adlips.application.view.ClaimedContentViews;
import glitched.adlips.application.view.ContentViewKey;
import java.util.List;

public interface ContentViewPendingPort {

    List<ContentViewKey> findPending(int limit);

    ClaimedContentViews claim(ContentViewKey key);

    void complete(ContentViewKey key, String batchId, long count);

    void restore(ContentViewKey key, String batchId, long count);
}
