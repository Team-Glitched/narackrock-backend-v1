package glitched.adlips.adapter.out.redis.view;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.application.view.ContentViewTarget;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
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
class RedisContentViewCounterAdapterTest {

    private static final String PASSWORD = "view-test-password";

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:8.10.1"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", PASSWORD);

    static LettuceConnectionFactory connectionFactory;
    static StringRedisTemplate redisTemplate;

    RedisContentViewCounterAdapter adapter;

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
        adapter = new RedisContentViewCounterAdapter(redisTemplate, "adlips-test");
    }

    @Test
    void 동일_조회자는_TTL_동안_한_번만_집계한다() {
        boolean first = adapter.recordIfFirst(
                ContentViewTarget.SHORT, 12L, "user:7", Duration.ofMinutes(30));
        boolean duplicate = adapter.recordIfFirst(
                ContentViewTarget.SHORT, 12L, "user:7", Duration.ofMinutes(30));

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();
        assertThat(redisTemplate.opsForValue().get(adapter.pendingKey(ContentViewTarget.SHORT, 12L)))
                .isEqualTo("1");
        assertThat(redisTemplate.opsForSet().isMember(
                adapter.dirtyKey(), adapter.member(ContentViewTarget.SHORT, 12L))).isTrue();
    }

    @Test
    void 동시_요청도_Redis에서_원자적으로_한_번만_집계한다() throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            Callable<Boolean> request = () -> adapter.recordIfFirst(
                    ContentViewTarget.POST, 501L, "device:hash", Duration.ofMinutes(30));
            List<Callable<Boolean>> requests = java.util.Collections.nCopies(20, request);

            long counted = executor.invokeAll(requests).stream()
                    .filter(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .count();

            assertThat(counted).isEqualTo(1);
            assertThat(redisTemplate.opsForValue().get(adapter.pendingKey(ContentViewTarget.POST, 501L)))
                    .isEqualTo("1");
        }
    }
}
