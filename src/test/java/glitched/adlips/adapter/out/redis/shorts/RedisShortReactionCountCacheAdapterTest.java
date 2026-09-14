package glitched.adlips.adapter.out.redis.shorts;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.shorts.ShortReactionCounts;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RedisShortReactionCountCacheAdapterTest {

    private static final String PASSWORD = "reaction-test-password";

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:8.10.1"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", PASSWORD);

    static LettuceConnectionFactory connectionFactory;
    static StringRedisTemplate redisTemplate;

    RedisShortReactionCountCacheAdapter adapter;

    @BeforeAll
    static void connect() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                REDIS.getHost(), REDIS.getMappedPort(6379));
        configuration.setPassword(RedisPassword.of(PASSWORD));
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        adapter = new RedisShortReactionCountCacheAdapter(
                redisTemplate, "adlips-test", Duration.ofMinutes(10));
    }

    @Test
    void storesAndReadsReactionCountsInOneBatch() {
        adapter.put(12L, new ShortReactionCounts(128, 4));
        adapter.put(13L, new ShortReactionCounts(7, 2));

        var result = adapter.findAll(List.of(12L, 13L, 14L));

        assertThat(result).containsEntry(12L, new ShortReactionCounts(128, 4));
        assertThat(result).containsEntry(13L, new ShortReactionCounts(7, 2));
        assertThat(result).doesNotContainKey(14L);
        assertThat(redisTemplate.getExpire(adapter.key(12L))).isPositive();
    }
}
