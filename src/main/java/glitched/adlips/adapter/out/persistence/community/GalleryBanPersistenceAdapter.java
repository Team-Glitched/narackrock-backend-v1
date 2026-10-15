package glitched.adlips.adapter.out.persistence.community;

import glitched.adlips.application.community.port.out.GalleryBanItem;
import glitched.adlips.application.community.port.out.GalleryBanPort;
import glitched.adlips.application.community.port.out.GalleryBanQueryPort;
import glitched.adlips.domain.community.GalleryBan;
import glitched.adlips.domain.community.GalleryBanType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class GalleryBanPersistenceAdapter implements GalleryBanPort, GalleryBanQueryPort {

    private final GalleryBanJpaRepository banRepository;

    public GalleryBanPersistenceAdapter(GalleryBanJpaRepository banRepository) {
        this.banRepository = banRepository;
    }

    @Override
    public boolean existsActive(Long galleryId, Long userId, LocalDateTime now) {
        return banRepository.existsActive(galleryId, userId, now);
    }

    @Override
    public Long save(GalleryBan ban) {
        return banRepository.save(ban).getId();
    }

    @Override
    public Optional<GalleryBan> findById(Long banId) {
        return banRepository.findById(banId);
    }

    @Override
    public void update(GalleryBan ban) {
        banRepository.save(ban);
    }

    @Override
    public List<GalleryBanItem> findPage(
            Long galleryId, boolean activeOnly, LocalDateTime now, Long cursor, int limit
    ) {
        return banRepository.findPage(galleryId, activeOnly, now, cursor, PageRequest.of(0, limit)).stream()
                .map(ban -> new GalleryBanItem(
                        ban.getId(),
                        ban.getUserId(),
                        ban.getActorId(),
                        ban.getBanType(),
                        ban.getReason(),
                        ban.getBannedUntil(),
                        ban.getLiftedAt(),
                        ban.getCreatedAt(),
                        ban.isActiveAt(now)))
                .toList();
    }

    @Override
    public boolean isRestrictedInGallery(Long galleryId, Long userId, LocalDateTime now) {
        return banRepository.existsActive(galleryId, userId, now);
    }

    @Override
    public boolean isRestrictedForPost(Long postId, Long userId, LocalDateTime now) {
        return banRepository.existsActiveForPost(postId, userId, now);
    }

    @Override
    public boolean isRestrictedForComment(Long commentId, Long userId, LocalDateTime now) {
        return banRepository.existsActiveForComment(commentId, userId, now);
    }

    @Override
    public boolean isExpelled(Long galleryId, Long userId, LocalDateTime now) {
        return banRepository.existsActiveOfType(galleryId, userId, GalleryBanType.EXPEL, now);
    }
}
