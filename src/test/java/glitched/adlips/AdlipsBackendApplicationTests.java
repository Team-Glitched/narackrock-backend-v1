package glitched.adlips;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.auth.token-secret=test-only-secret-value-32-characters")
class AdlipsBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
