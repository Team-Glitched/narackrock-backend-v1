package glitched.adlips.adapter.out.redis.view;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import glitched.adlips.application.view.ContentViewApplicationException;
import glitched.adlips.application.view.ContentViewErrorCode;
import glitched.adlips.application.view.ContentViewTarget;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RedisContentViewCounterAdapterFailureTest {

    @Mock StringRedisTemplate redisTemplate;

    @Test
    void Redis_연결_실패를_서비스_이용불가_예외로_변환한다() {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("redis unavailable"));
        RedisContentViewCounterAdapter adapter =
                new RedisContentViewCounterAdapter(redisTemplate, "adlips-test");

        assertThatThrownBy(() -> adapter.recordIfFirst(
                ContentViewTarget.SHORT, 12L, "user:7", Duration.ofMinutes(30)))
                .isInstanceOf(ContentViewApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(ContentViewErrorCode.VIEW_COUNT_UNAVAILABLE);
    }
}
