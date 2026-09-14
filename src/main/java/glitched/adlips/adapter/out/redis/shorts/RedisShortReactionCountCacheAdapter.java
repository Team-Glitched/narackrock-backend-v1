package glitched.adlips.adapter.out.redis.shorts;

import glitched.adlips.application.shorts.ShortReactionCounts;
import glitched.adlips.application.shorts.port.out.ShortReactionCountCachePort;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisShortReactionCountCacheAdapter implements ShortReactionCountCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisShortReactionCountCacheAdapter.class);

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final Duration ttl;

    @Autowired
    public RedisShortReactionCountCacheAdapter(
            StringRedisTemplate redisTemplate,
            @Value("${app.redis.short-reaction-cache-ttl:PT10M}") Duration ttl
    ) {
        this(redisTemplate, "adlips", ttl);
    }

    RedisShortReactionCountCacheAdapter(
            StringRedisTemplate redisTemplate,
            String keyPrefix,
            Duration ttl
    ) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.keyPrefix = normalizePrefix(keyPrefix);
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.ttl = ttl;
    }

    @Override
    public Map<Long, ShortReactionCounts> findAll(List<Long> shortIds) {
        if (shortIds == null || shortIds.isEmpty()) {
            return Map.of();
        }
        List<String> keys = shortIds.stream().map(this::key).toList();
        try {
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                return Map.of();
            }
            Map<Long, ShortReactionCounts> result = new LinkedHashMap<>();
            for (int index = 0; index < shortIds.size() && index < values.size(); index++) {
                ShortReactionCounts counts = parse(values.get(index));
                if (counts != null) {
                    result.put(shortIds.get(index), counts);
                }
            }
            return Map.copyOf(result);
        } catch (DataAccessException exception) {
            log.warn("Redis 숏폼 반응 수 조회 실패, DB 값으로 대체합니다.", exception);
            return Map.of();
        }
    }

    @Override
    public void put(Long shortId, ShortReactionCounts counts) {
        Objects.requireNonNull(shortId, "shortId must not be null");
        Objects.requireNonNull(counts, "counts must not be null");
        try {
            redisTemplate.opsForValue().set(
                    key(shortId), counts.likeCount() + ":" + counts.dislikeCount(), ttl);
        } catch (DataAccessException exception) {
            log.warn("Redis 숏폼 반응 수 갱신 실패: shortId={}", shortId, exception);
        }
    }

    String key(Long shortId) {
        return keyPrefix + ":shorts:reaction-counts:" + shortId;
    }

    private ShortReactionCounts parse(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new ShortReactionCounts(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String normalizePrefix(String prefix) {
        Objects.requireNonNull(prefix, "keyPrefix must not be null");
        String normalized = prefix.trim();
        while (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("keyPrefix must not be blank");
        }
        return normalized;
    }
}
