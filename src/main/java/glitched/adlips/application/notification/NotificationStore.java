package glitched.adlips.application.notification;

import glitched.adlips.domain.notification.*;

import java.time.Instant;
import java.util.List;

public interface NotificationStore {
    boolean insert(NotificationDraft draft);

    List<ServiceNotification> list(
            Long userId, Long cursor, String category, boolean unreadOnly, int size);

    long unread(Long userId);

    int markRead(Long userId, Long id, Instant now);

    int markAllRead(Long userId, Long throughId, Instant now);

    int deleteRead(Long userId);

    NotificationPreferences.Values preferences(Long userId);

    void preferences(Long userId, NotificationPreferences.Values values);
}
