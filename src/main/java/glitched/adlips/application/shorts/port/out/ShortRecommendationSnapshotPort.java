package glitched.adlips.application.shorts.port.out;

import glitched.adlips.application.shorts.RecommendationReason;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShortRecommendationSnapshotPort {
    record Entry(Long shortId, RecommendationReason reason) {}

    void save(Long userId, UUID id, List<Entry> entries);

    Optional<List<Entry>> find(Long userId, UUID id);
}
