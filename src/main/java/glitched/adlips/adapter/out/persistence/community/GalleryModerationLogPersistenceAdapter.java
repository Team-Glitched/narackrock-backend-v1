package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.GalleryModerationLogItem;
import glitched.adlips.application.community.port.out.GalleryModerationLogPort;
import glitched.adlips.domain.community.Gallery;
import glitched.adlips.domain.community.GalleryModerationActionType;
import glitched.adlips.domain.community.GalleryModerationLog;
import glitched.adlips.domain.user.User;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class GalleryModerationLogPersistenceAdapter implements GalleryModerationLogPort {

    private final GalleryModerationLogJpaRepository logRepository;
    private final EntityManager entityManager;

    public GalleryModerationLogPersistenceAdapter(
            GalleryModerationLogJpaRepository logRepository,
            EntityManager entityManager
    ) {
        this.logRepository = logRepository;
        this.entityManager = entityManager;
    }

    @Override
    public void save(
            Long galleryId,
            Long actorId,
            GalleryModerationActionType actionType,
            Long targetUserId,
            Long targetId,
            String reason
    ) {
        logRepository.save(GalleryModerationLog.record(
                entityManager.getReference(Gallery.class, galleryId),
                entityManager.getReference(User.class, actorId),
                actionType,
                targetUserId,
                targetId,
                reason));
    }

    @Override
    public List<GalleryModerationLogItem> findPage(Long galleryId, Long cursor, int limit) {
        return logRepository.findPage(galleryId, cursor, PageRequest.of(0, limit)).stream()
                .map(log -> new GalleryModerationLogItem(
                        log.getId(),
                        log.getActionType(),
                        log.getActorId(),
                        log.getTargetUserId(),
                        log.getTargetId(),
                        log.getReason(),
                        log.getCreatedAt()))
                .toList();
    }
}
