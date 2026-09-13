package glitched.adlips.adapter.out.redis.view;

import glitched.adlips.application.view.ContentViewTarget;
import glitched.adlips.application.view.port.out.ContentViewCounterPort;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisContentViewCounterAdapter implements ContentViewCounterPort {

    private static final DefaultRedisScript<Long> RECORD_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[1]) then
                redis.call('INCR', KEYS[2])
                redis.call('SADD', KEYS[3], ARGV[2])
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisContentViewCounterAdapter(StringRedisTemplate redisTemplate) {
        this(redisTemplate, "adlips");
    }

    RedisContentViewCounterAdapter(StringRedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.keyPrefix = normalizePrefix(keyPrefix);
    }

    @Override
    public boolean recordIfFirst(
            ContentViewTarget target,
            Long contentId,
            String viewerId,
            Duration deduplicationWindow
    ) {
        Long result = redisTemplate.execute(
                RECORD_SCRIPT,
                List.of(
                        deduplicationKey(target, contentId, viewerId),
                        pendingKey(target, contentId),
                        dirtyKey()
                ),
                Long.toString(deduplicationWindow.toSeconds()),
                member(target, contentId)
        );
        return Long.valueOf(1L).equals(result);
    }

    String pendingKey(ContentViewTarget target, Long contentId) {
        return "%s:views:pending:%s:%d".formatted(keyPrefix, targetName(target), contentId);
    }

    String dirtyKey() {
        return keyPrefix + ":views:dirty";
    }

    String member(ContentViewTarget target, Long contentId) {
        return "%s:%d".formatted(targetName(target), contentId);
    }

    private String deduplicationKey(ContentViewTarget target, Long contentId, String viewerId) {
        return "%s:views:dedupe:%s:%d:%s"
                .formatted(keyPrefix, targetName(target), contentId, viewerId);
    }

    private static String targetName(ContentViewTarget target) {
        return target.name().toLowerCase(Locale.ROOT);
    }

    private static String normalizePrefix(String prefix) {
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
