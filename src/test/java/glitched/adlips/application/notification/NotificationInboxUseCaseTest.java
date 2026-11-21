package glitched.adlips.application.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.UserApplicationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class NotificationInboxUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");

    @Test
    void deleteRejectsInvalidIdsAndNotifiesDeliveryAfterSoftDelete() {
        NotificationStore store = mock(NotificationStore.class);
        NotificationAudiencePort audiences = mock(NotificationAudiencePort.class);
        NotificationDeliveryPort delivery = mock(NotificationDeliveryPort.class);
        var inbox = new NotificationInboxUseCase(
                store, audiences, TransactionRunner.direct(), delivery, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> inbox.delete(1L, null)).isInstanceOf(UserApplicationException.class);
        assertThatThrownBy(() -> inbox.delete(1L, 0L)).isInstanceOf(UserApplicationException.class);
        verifyNoInteractions(store, delivery);

        inbox.delete(7L, 42L);
        verify(store).deleteOne(7L, 42L, NOW);
        verify(delivery).changed(7L);

        inbox.deleteRead(7L);
        verify(store).deleteRead(7L, NOW);
    }
}
