package glitched.adlips.adapter.out.redis.shorts;

import glitched.adlips.application.shorts.port.out.ShortPopularityRankingPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisShortPopularityRankingAdapter implements ShortPopularityRankingPort {
    private static final Logger log = LoggerFactory.getLogger(RedisShortPopularityRankingAdapter.class);
    private static final double LIKE_WEIGHT = 5.0;
    private static final DefaultRedisScript<Long> SEED_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('SADD', KEYS[3], ARGV[1]) == 1 then
                redis.call('ZINCRBY', KEYS[1], ARGV[2], ARGV[1])
                redis.call('ZADD', KEYS[2], 'NX', ARGV[3], ARGV[1])
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final String viewRankingKey;
    private final String likeRankingKey;
    private final String combinedRankingKey;
    private final String seededKey;

    public RedisShortPopularityRankingAdapter(StringRedisTemplate redisTemplate) {
        this(redisTemplate, "adlips");
    }

    RedisShortPopularityRankingAdapter(StringRedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        String prefix = normalizePrefix(keyPrefix);
        this.viewRankingKey = prefix + ":shorts:popularity:views";
        this.likeRankingKey = prefix + ":shorts:popularity:likes";
        this.combinedRankingKey = prefix + ":shorts:popularity";
        this.seededKey = prefix + ":shorts:popularity:seeded";
    }

    @Override
    public List<Long> findTop(int limit) {
        if (limit <= 0) return List.of();
        try {
            redisTemplate.opsForZSet().unionAndStore(
                    viewRankingKey, List.of(likeRankingKey), combinedRankingKey);
            Set<String> values = redisTemplate.opsForZSet().reverseRange(combinedRankingKey, 0, limit - 1L);
            if (values == null || values.isEmpty()) return List.of();
            List<Long> result = new ArrayList<>(values.size());
            for (String value : values) {
                try {
                    result.add(Long.parseLong(value));
                } catch (NumberFormatException exception) {
                    redisTemplate.opsForZSet().remove(combinedRankingKey, value);
                }
            }
            return List.copyOf(result);
        } catch (DataAccessException exception) {
            log.warn("Redis 인기곡 순위 조회 실패", exception);
            return List.of();
        }
    }

    @Override
    public void seedBaseline(List<glitched.adlips.application.shorts.port.out.ShortPopularityScore> scores) {
        if (scores == null || scores.isEmpty()) return;
        try {
            scores.forEach(score -> redisTemplate.execute(
                    SEED_SCRIPT,
                    List.of(viewRankingKey, likeRankingKey, seededKey),
                    score.shortId().toString(),
                    Double.toString(score.viewCount() + score.recencyTieBreaker()),
                    Double.toString(score.likeCount() * LIKE_WEIGHT)));
        } catch (DataAccessException exception) {
            log.warn("Redis 인기곡 기준 점수 초기화 실패", exception);
        }
    }

    @Override
    public void incrementView(Long shortId) {
        increment(viewRankingKey, shortId, 1.0);
    }

    @Override
    public void updateLikeCount(Long shortId, int likeCount) {
        try {
            redisTemplate.opsForZSet().add(likeRankingKey, shortId.toString(), likeCount * LIKE_WEIGHT);
        } catch (DataAccessException exception) {
            log.warn("Redis 인기곡 좋아요 점수 갱신 실패: shortId={}", shortId, exception);
        }
    }

    private void increment(String key, Long shortId, double delta) {
        try {
            redisTemplate.opsForZSet().incrementScore(key, shortId.toString(), delta);
        } catch (DataAccessException exception) {
            log.warn("Redis 인기곡 점수 갱신 실패: shortId={}", shortId, exception);
        }
    }

    private static String normalizePrefix(String prefix) {
        String normalized = Objects.requireNonNull(prefix).trim();
        while (normalized.endsWith(":")) normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.isBlank()) throw new IllegalArgumentException("keyPrefix must not be blank");
        return normalized;
    }
}
