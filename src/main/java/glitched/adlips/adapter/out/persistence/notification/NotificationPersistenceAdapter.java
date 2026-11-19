package glitched.adlips.adapter.out.persistence.notification;

import glitched.adlips.application.notification.*;
import glitched.adlips.domain.notification.*;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class NotificationPersistenceAdapter implements NotificationStore {
    private final EntityManager em;
    private final NotificationJpaRepository repository;

    public NotificationPersistenceAdapter(EntityManager em, NotificationJpaRepository repository) {
        this.em = em;
        this.repository = repository;
    }

    public boolean insert(NotificationDraft draft) {
        // Serialize concurrent deliveries to one recipient, including duplicate HTTP retries.
        em.createNativeQuery("select id from users where id=:id for update", Long.class)
                .setParameter("id", draft.recipientId())
                .getResultList();
        if (repository.existsByRecipientIdAndEventKey(draft.recipientId(), draft.eventKey()))
            return false;
        repository.save(draft.entity());
        return true;
    }

    public List<ServiceNotification> list(
            Long userId, Long cursor, String category, boolean unreadOnly, int size) {
        String where =
                "n.recipientId=:user"
                        + (cursor == null ? "" : " and n.id<:cursor")
                        + (category == null ? "" : " and n.type in :types")
                        + (unreadOnly ? " and n.readAt is null" : "");
        var q =
                em.createQuery(
                                "select n from ServiceNotification n where "
                                        + where
                                        + " order by n.id desc",
                                ServiceNotification.class)
                        .setParameter("user", userId);
        if (cursor != null) q.setParameter("cursor", cursor);
        if (category != null)
            q.setParameter(
                    "types",
                    java.util.Arrays.stream(NotificationType.values())
                            .filter(t -> t.category.equals(category))
                            .toList());
        return q.setMaxResults(size).getResultList();
    }

    public long unread(Long userId) {
        return em.createQuery(
                        "select count(n) from ServiceNotification n where n.recipientId=:user and"
                            + " n.readAt is null",
                        Long.class)
                .setParameter("user", userId)
                .getSingleResult();
    }

    public int markRead(Long userId, Long id, Instant now) {
        return em.createQuery(
                        "update ServiceNotification n set n.readAt=:now where n.recipientId=:user"
                            + " and n.id=:id and n.readAt is null")
                .setParameter("now", now)
                .setParameter("user", userId)
                .setParameter("id", id)
                .executeUpdate();
    }

    public int markAllRead(Long userId, Long throughId, Instant now) {
        return em.createQuery(
                        "update ServiceNotification n set n.readAt=:now where n.recipientId=:user"
                            + " and n.id<=:id and n.readAt is null")
                .setParameter("now", now)
                .setParameter("user", userId)
                .setParameter("id", throughId)
                .executeUpdate();
    }

    public int deleteRead(Long userId) {
        return em.createQuery(
                        "delete from ServiceNotification n where n.recipientId=:user and n.readAt is not null")
                .setParameter("user", userId)
                .executeUpdate();
    }

    public NotificationPreferences.Values preferences(Long userId) {
        var p = em.find(NotificationPreferences.class, userId);
        return p == null ? NotificationPreferences.Values.defaults() : p.values();
    }

    public void preferences(Long userId, NotificationPreferences.Values values) {
        var p = em.find(NotificationPreferences.class, userId);
        if (p == null) em.persist(new NotificationPreferences(userId, values));
        else p.set(values);
    }
}
