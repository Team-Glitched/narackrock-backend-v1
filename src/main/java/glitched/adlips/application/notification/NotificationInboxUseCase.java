package glitched.adlips.application.notification;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.*;
import glitched.adlips.domain.notification.*;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

public class NotificationInboxUseCase {
    public record Item(
            Long id,
            String type,
            String category,
            Long actorId,
            String title,
            String body,
            String path,
            Instant createdAt,
            Instant readAt,
            boolean available,
            Long projectId,
            Long requestId,
            String requestStatus) {}

    public record Page(List<Item> items, Long nextCursor, long unreadCount, Long throughId) {}

    private final NotificationEvents events;
    private final NotificationStore store;
    private final NotificationAudiencePort audiences;
    private final TransactionRunner transactions;
    private final NotificationDeliveryPort delivery;
    private final Clock clock;

    public NotificationInboxUseCase(
            NotificationStore store,
            NotificationAudiencePort audiences,
            TransactionRunner transactions,
            NotificationDeliveryPort delivery,
            Clock clock) {
        this(store, audiences, transactions, delivery, clock, NotificationEvents.none());
    }

    public NotificationInboxUseCase(
            NotificationStore store,
            NotificationAudiencePort audiences,
            TransactionRunner transactions,
            NotificationDeliveryPort delivery,
            Clock clock,
            NotificationEvents events) {
        this.events = events;
        this.store = store;
        this.audiences = audiences;
        this.transactions = transactions;
        this.delivery = delivery;
        this.clock = clock;
    }

    public Page list(Long user, Long cursor, String category, boolean unreadOnly, int size) {
        if (size < 1
                || size > 100
                || (cursor != null && cursor < 1)
                || (category != null
                        && Arrays.stream(NotificationType.values())
                                .noneMatch(t -> t.category.equals(category)))) throw invalid();
        return transactions.required(
                () -> {
                    synchronize(user);
                    var all = store.list(user, cursor, category, unreadOnly, size + 1);
                    var items =
                            all.stream()
                                    .limit(size)
                                    .map(
                                            n -> {
                                                var access = audiences.access(user, n);
                                                return new Item(
                                                        n.getId(),
                                                        n.getType().name(),
                                                        n.getType().category,
                                                        n.getActorId(),
                                                        n.getTitle(),
                                                        access.available()
                                                                ? n.getBody()
                                                                : "삭제되었거나 현재 접근할 수 없는 내용입니다.",
                                                        access.available() ? n.getPath() : null,
                                                        n.getCreatedAt(),
                                                        n.getReadAt(),
                                                        access.available(),
                                                        n.getProjectId(),
                                                        n.getRequestId(),
                                                        access.requestStatus());
                                            })
                                    .toList();
                    var newest = store.list(user, null, null, false, 1);
                    return new Page(
                            items,
                            all.size() > size ? items.getLast().id() : null,
                            store.unread(user),
                            newest.isEmpty() ? null : newest.getFirst().getId());
                });
    }

    private void synchronize(Long user) {
        for (var pending : audiences.pending(user))
            events.record(
                    pending.type(),
                    pending.actorId(),
                    pending.targetId(),
                    pending.referenceId(),
                    null);
    }

    public long unread(Long user) {
        return transactions.required(
                () -> {
                    synchronize(user);
                    return store.unread(user);
                });
    }

    public void read(Long user, Long id) {
        if (id == null || id < 1) throw invalid();
        transactions.required(
                () -> {
                    store.markRead(user, id, clock.instant());
                    delivery.changed(user);
                    return null;
                });
    }

    public void readAll(Long user, Long through) {
        if (through == null || through < 1) throw invalid();
        transactions.required(
                () -> {
                    store.markAllRead(user, through, clock.instant());
                    delivery.changed(user);
                    return null;
                });
    }

    public void deleteRead(Long user) {
        transactions.required(
                () -> {
                    store.deleteRead(user);
                    delivery.changed(user);
                    return null;
                });
    }

    public NotificationPreferences.Values preferences(Long user) {
        return transactions.readOnly(() -> store.preferences(user));
    }

    public void preferences(Long user, NotificationPreferences.Values values) {
        if (values == null) throw invalid();
        transactions.required(
                () -> {
                    store.preferences(user, values);
                    return null;
                });
    }

    private UserApplicationException invalid() {
        return new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "알림 요청값을 확인해 주세요.");
    }
}
