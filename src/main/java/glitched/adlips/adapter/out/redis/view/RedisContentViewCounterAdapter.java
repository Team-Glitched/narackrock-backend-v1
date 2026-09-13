package glitched.adlips.adapter.out.redis.view;

import glitched.adlips.application.view.ContentViewTarget;
import glitched.adlips.application.view.ClaimedContentViews;
import glitched.adlips.application.view.ContentViewKey;
import glitched.adlips.application.view.port.out.ContentViewCounterPort;
import glitched.adlips.application.view.port.out.ContentViewPendingPort;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisContentViewCounterAdapter implements ContentViewCounterPort, ContentViewPendingPort {

    private static final DefaultRedisScript<Long> RECORD_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[1]) then
                redis.call('INCR', KEYS[2])
                redis.call('SADD', KEYS[3], ARGV[2])
                return 1
            end
            return 0
            """, Long.class);

    private static final DefaultRedisScript<String> CLAIM_SCRIPT = new DefaultRedisScript<>("""
            local processing = redis.call('GET', KEYS[2])
            if processing then
                return processing
            end
            local pending = tonumber(redis.call('GET', KEYS[1]) or '0')
            if pending <= 0 then
                redis.call('DEL', KEYS[1])
                redis.call('SREM', KEYS[3], ARGV[2])
                return ''
            end
            local claimed = ARGV[1] .. ':' .. pending
            redis.call('SET', KEYS[2], claimed)
            return claimed
            """, String.class);

    private static final DefaultRedisScript<Long> COMPLETE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[2]) ~= ARGV[1] then
                return -1
            end
            local remaining = redis.call('DECRBY', KEYS[1], ARGV[2])
            redis.call('DEL', KEYS[2])
            if remaining <= 0 then
                redis.call('DEL', KEYS[1])
                redis.call('SREM', KEYS[3], ARGV[3])
                return 0
            end
            return remaining
            """, Long.class);

    private static final DefaultRedisScript<Long> RESTORE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final Supplier<String> batchIdSupplier;

    public RedisContentViewCounterAdapter(StringRedisTemplate redisTemplate) {
        this(redisTemplate, "adlips", () -> UUID.randomUUID().toString());
    }

    RedisContentViewCounterAdapter(StringRedisTemplate redisTemplate, String keyPrefix) {
        this(redisTemplate, keyPrefix, () -> UUID.randomUUID().toString());
    }

    RedisContentViewCounterAdapter(
            StringRedisTemplate redisTemplate,
            String keyPrefix,
            Supplier<String> batchIdSupplier
    ) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.keyPrefix = normalizePrefix(keyPrefix);
        this.batchIdSupplier = Objects.requireNonNull(batchIdSupplier);
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

    @Override
    public List<ContentViewKey> findPending(int limit) {
        Set<String> members = redisTemplate.opsForSet()
                .distinctRandomMembers(dirtyKey(), limit);
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        List<ContentViewKey> keys = new ArrayList<>(members.size());
        for (String member : members) {
            ContentViewKey key = parseMember(member);
            if (key != null) {
                keys.add(key);
            } else {
                redisTemplate.opsForSet().remove(dirtyKey(), member);
            }
        }
        return List.copyOf(keys);
    }

    @Override
    public ClaimedContentViews claim(ContentViewKey key) {
        String claimed = redisTemplate.execute(
                CLAIM_SCRIPT,
                List.of(pendingKey(key.target(), key.contentId()), processingKey(key), dirtyKey()),
                batchIdSupplier.get(),
                member(key.target(), key.contentId())
        );
        if (claimed == null || claimed.isBlank()) {
            return ClaimedContentViews.empty();
        }
        int separator = claimed.lastIndexOf(':');
        if (separator <= 0 || separator == claimed.length() - 1) {
            throw new IllegalStateException("Invalid claimed view count value");
        }
        return new ClaimedContentViews(
                claimed.substring(0, separator),
                Long.parseLong(claimed.substring(separator + 1))
        );
    }

    @Override
    public void complete(ContentViewKey key, String batchId, long count) {
        redisTemplate.execute(
                COMPLETE_SCRIPT,
                List.of(pendingKey(key.target(), key.contentId()), processingKey(key), dirtyKey()),
                claimedValue(batchId, count),
                Long.toString(count),
                member(key.target(), key.contentId())
        );
    }

    @Override
    public void restore(ContentViewKey key, String batchId, long count) {
        redisTemplate.execute(
                RESTORE_SCRIPT,
                List.of(processingKey(key)),
                claimedValue(batchId, count)
        );
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

    String processingKey(ContentViewKey key) {
        return "%s:views:processing:%s:%d"
                .formatted(keyPrefix, targetName(key.target()), key.contentId());
    }

    private String deduplicationKey(ContentViewTarget target, Long contentId, String viewerId) {
        return "%s:views:dedupe:%s:%d:%s"
                .formatted(keyPrefix, targetName(target), contentId, viewerId);
    }

    private static String targetName(ContentViewTarget target) {
        return target.name().toLowerCase(Locale.ROOT);
    }

    private static String claimedValue(String batchId, long count) {
        return batchId + ":" + count;
    }

    private static ContentViewKey parseMember(String member) {
        String[] parts = member.split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new ContentViewKey(
                    ContentViewTarget.valueOf(parts[0].toUpperCase(Locale.ROOT)),
                    Long.parseLong(parts[1])
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
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
