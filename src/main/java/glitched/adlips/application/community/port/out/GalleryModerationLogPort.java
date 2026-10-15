package glitched.adlips.application.community.port.out;

import glitched.adlips.domain.community.GalleryModerationActionType;
import java.util.List;

public interface GalleryModerationLogPort {
    void save(
            Long galleryId,
            Long actorId,
            GalleryModerationActionType actionType,
            Long targetUserId,
            Long targetId,
            String reason
    );

    /** cursor가 null이면 처음부터. id 내림차순. */
    List<GalleryModerationLogItem> findPage(Long galleryId, Long cursor, int limit);
}
