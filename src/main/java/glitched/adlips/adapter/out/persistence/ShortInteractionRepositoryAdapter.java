package glitched.adlips.adapter.out.persistence;

import glitched.adlips.application.port.ShortInteractionRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
class ShortInteractionRepositoryAdapter implements ShortInteractionRepository {

    private final ShortLikeJpaRepository likeRepository;
    private final ShortDislikeJpaRepository dislikeRepository;
    private final ShortBookmarkJpaRepository bookmarkRepository;

    ShortInteractionRepositoryAdapter(
            ShortLikeJpaRepository likeRepository,
            ShortDislikeJpaRepository dislikeRepository,
            ShortBookmarkJpaRepository bookmarkRepository
    ) {
        this.likeRepository = likeRepository;
        this.dislikeRepository = dislikeRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    @Override
    public Set<Long> findLikedShortIds(Long userId, List<Long> shortIds) {
        return likeRepository.findShortIdsByUserIdAndShortIdIn(userId, shortIds);
    }

    @Override
    public Set<Long> findDislikedShortIds(Long userId, List<Long> shortIds) {
        return dislikeRepository.findShortIdsByUserIdAndShortIdIn(userId, shortIds);
    }

    @Override
    public Set<Long> findBookmarkedShortIds(Long userId, List<Long> shortIds) {
        return bookmarkRepository.findShortIdsByUserIdAndShortIdIn(userId, shortIds);
    }
}
