package glitched.adlips.adapter.out.redis.shorts;

import glitched.adlips.application.shorts.port.out.ShortRecommendationSnapshotPort;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisShortRecommendationSnapshotAdapter implements ShortRecommendationSnapshotPort {
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;
    private final String prefix;

    public RedisShortRecommendationSnapshotAdapter(
            StringRedisTemplate redis, @Value("${app.redis.key-prefix:narackrock}") String prefix
    ) {
        this.redis = redis;
        this.prefix = prefix.trim().replaceFirst(":+$", "");
    }

    @Override
    public void save(Long userId, UUID id, List<Long> shortIds) {
        String value = shortIds.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
        redis.opsForValue().set(key(userId, id), value, TTL);
    }

    @Override
    public Optional<List<Long>> find(Long userId, UUID id) {
        return Optional.ofNullable(redis.opsForValue().get(key(userId, id))).map(value ->
                Arrays.stream(value.split(",")).map(Long::valueOf).toList());
    }

    private String key(Long userId, UUID id) {
        return prefix + ":shorts:recommendations:" + (userId == null ? "guest" : userId) + ":" + id;
    }
}
