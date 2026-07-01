package glitched.adlips;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(properties = {
        "app.auth.token-secret=test-only-secret-value-32-characters",
        "spring.datasource.url=jdbc:h2:mem:adlips;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AdlipsBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void postgresqlDriverIsAvailable() {
        assertDoesNotThrow(() -> Class.forName("org.postgresql.Driver"));
    }

}
