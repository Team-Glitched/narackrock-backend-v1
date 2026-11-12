package glitched.adlips.application.shorts.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShortRecommendationSnapshotPort {
    void save(Long userId, UUID id, List<Long> shortIds);

    Optional<List<Long>> find(Long userId, UUID id);
}
