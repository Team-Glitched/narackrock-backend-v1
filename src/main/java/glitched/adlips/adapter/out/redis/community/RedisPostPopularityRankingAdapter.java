package glitched.adlips.adapter.out.redis.community;

import glitched.adlips.application.community.port.out.PostPopularityRankingPort;
import glitched.adlips.application.community.port.out.PostPopularityScore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisPostPopularityRankingAdapter implements PostPopularityRankingPort {
    private static final Logger log = LoggerFactory.getLogger(RedisPostPopularityRankingAdapter.class);
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

    @Autowired
    public RedisPostPopularityRankingAdapter(StringRedisTemplate redisTemplate) {
        this(redisTemplate, "adlips");
    }

    RedisPostPopularityRankingAdapter(StringRedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        String prefix = Objects.requireNonNull(keyPrefix).strip();
        while (prefix.endsWith(":")) prefix = prefix.substring(0, prefix.length() - 1);
        if (prefix.isBlank()) throw new IllegalArgumentException("keyPrefix must not be blank");
        this.viewRankingKey = prefix + ":posts:popularity:views";
        this.likeRankingKey = prefix + ":posts:popularity:likes";
        this.combinedRankingKey = prefix + ":posts:popularity";
        this.seededKey = prefix + ":posts:popularity:seeded";
    }

    @Override
    public void seedBaseline(List<PostPopularityScore> scores) {
        if (scores == null || scores.isEmpty()) return;
        try {
            scores.forEach(score -> redisTemplate.execute(
                    SEED_SCRIPT,
                    List.of(viewRankingKey, likeRankingKey, seededKey),
                    score.postId().toString(),
                    Double.toString(score.viewCount() + score.recencyTieBreaker()),
                    Double.toString(score.likeCount() * LIKE_WEIGHT)));
        } catch (DataAccessException exception) {
            log.warn("Redis 인기글 기준 점수 초기화 실패", exception);
        }
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
            log.warn("Redis 인기글 순위 조회 실패", exception);
            return List.of();
        }
    }

    @Override
    public void updateLikeCount(Long postId, int likeCount) {
        try {
            redisTemplate.opsForZSet().add(likeRankingKey, postId.toString(), likeCount * LIKE_WEIGHT);
        } catch (DataAccessException exception) {
            log.warn("Redis 인기글 좋아요 점수 갱신 실패: postId={}", postId, exception);
        }
    }

    @Override
    public void remove(Long postId) {
        try {
            String member = postId.toString();
            redisTemplate.opsForZSet().remove(viewRankingKey, member);
            redisTemplate.opsForZSet().remove(likeRankingKey, member);
            redisTemplate.opsForZSet().remove(combinedRankingKey, member);
            redisTemplate.opsForSet().remove(seededKey, member);
        } catch (DataAccessException exception) {
            log.warn("Redis 인기글 순위 삭제 실패: postId={}", postId, exception);
        }
    }
}
