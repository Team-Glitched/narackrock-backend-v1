package glitched.adlips.adapter.out.persistence.notification;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.notification.*;
import glitched.adlips.domain.notification.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

@DataJpaTest
@Import(NotificationPersistenceAdapter.class)
class NotificationPersistenceAdapterTest {
    @Autowired NotificationPersistenceAdapter store;

    private NotificationDraft draft(long user, String key) {
        return new NotificationDraft(
                user,
                NotificationType.FOLLOW_CREATED,
                7L,
                "새 팔로워",
                "사용자님이 팔로우했습니다.",
                "/users/7",
                "USER",
                7L,
                null,
                null,
                key,
                Instant.now());
    }

    @Test
    void notificationsAreDurableDeduplicatedAndPrivateToRecipient() {
        assertThat(store.insert(draft(1, "follow:7:1"))).isTrue();
        assertThat(store.insert(draft(1, "follow:7:1"))).isFalse();
        assertThat(store.list(1L, null, null, false, 20)).hasSize(1);
        assertThat(store.list(2L, null, null, false, 20)).isEmpty();
        Long id = store.list(1L, null, null, false, 20).getFirst().getId();
        assertThat(store.markRead(2L, id, Instant.now())).isZero();
        assertThat(store.unread(1L)).isEqualTo(1);
        assertThat(store.markRead(1L, id, Instant.now())).isEqualTo(1);
        assertThat(store.unread(1L)).isZero();
    }

    @Test
    void readAllUsesSnapshotSoNewNotificationsRemainUnread() {
        store.insert(draft(1, "a"));
        Long through = store.list(1L, null, null, false, 20).getFirst().getId();
        store.insert(draft(1, "b"));
        store.markAllRead(1L, through, Instant.now());
        assertThat(store.unread(1L)).isEqualTo(1);
    }

    @Test
    void preferencesPersistAndOptionalBroadcastsAreDisabledByDefault() {
        assertThat(store.preferences(1L).followingContent()).isFalse();
        assertThat(store.preferences(1L).reactions()).isTrue();
        store.preferences(1L, new NotificationPreferences.Values(false, true, true, true));
        assertThat(store.preferences(1L))
                .isEqualTo(new NotificationPreferences.Values(false, true, true, true));
        assertThat(store.preferences(2L).followingContent()).isFalse();
    }
}
