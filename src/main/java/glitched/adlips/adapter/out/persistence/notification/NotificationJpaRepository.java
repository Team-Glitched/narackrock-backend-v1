package glitched.adlips.adapter.out.persistence.notification;

import glitched.adlips.domain.notification.ServiceNotification;

import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationJpaRepository extends JpaRepository<ServiceNotification, Long> {
    boolean existsByRecipientIdAndEventKey(Long recipientId, String eventKey);
}
