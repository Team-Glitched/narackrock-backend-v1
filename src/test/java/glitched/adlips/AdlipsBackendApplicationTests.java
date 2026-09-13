package glitched.adlips;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "app.auth.token-secret=test-only-secret-value-32-characters",
        "spring.datasource.url=jdbc:h2:mem:adlips;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AdlipsBackendApplicationTests {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void contextLoads() {
    }

    @Test
    void postgresqlDriverIsAvailable() {
        assertDoesNotThrow(() -> Class.forName("org.postgresql.Driver"));
    }

    @Test
    void redisConnectionFactoryIsConfigured() {
        assertNotNull(redisConnectionFactory);
    }

}
