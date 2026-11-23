package glitched.adlips.application.opensource;

import glitched.adlips.domain.opensource.OpenSourcePublication;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OpenSourceQueryPort {
    record Writer(Long userId, String nickname, String profileImageUrl) {}
    record Item(Long openSourceId, String title, String description, String thumbnailUrl,
                Writer writer, boolean isSaved, Instant createdAt, boolean canPreview, boolean isLiked, boolean isDisliked, long likeCount, long dislikeCount, long commentCount, long usageCount, String instrument) {}
    record SavedItem(Long openSourceId, Instant savedAt, Item source) {}

    List<Item> list(Long viewerId, String keyword, int offset, int limit);
    List<Item> popular(Long viewerId, int limit);
    Optional<Item> detail(Long viewerId, Long openSourceId);
    List<SavedItem> saved(Long viewerId, int offset, int limit);
    List<OpenSourcePublication> publications(Long ownerId, int offset, int limit);
}
