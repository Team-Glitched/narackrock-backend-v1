package glitched.adlips.adapter.out.redis.shorts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import glitched.adlips.application.shorts.ShortReactionCounts;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisShortReactionCountCacheAdapterFailureTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> values;

    @Test
    void fallsBackToCacheMissWhenRedisReadFails() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.multiGet(any())).thenThrow(new DataAccessResourceFailureException("unavailable"));
        RedisShortReactionCountCacheAdapter adapter = adapter();

        assertThat(adapter.findAll(List.of(12L))).isEmpty();
    }

    @Test
    void doesNotFailRequestWhenRedisWriteFails() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        org.mockito.Mockito.doThrow(new DataAccessResourceFailureException("unavailable"))
                .when(values).set(any(), any(), any(Duration.class));
        RedisShortReactionCountCacheAdapter adapter = adapter();

        assertThatCode(() -> adapter.put(12L, new ShortReactionCounts(3, 1)))
                .doesNotThrowAnyException();
    }

    private RedisShortReactionCountCacheAdapter adapter() {
        return new RedisShortReactionCountCacheAdapter(
                redisTemplate, "adlips-test", Duration.ofMinutes(10));
    }
}
