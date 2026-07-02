package glitched.adlips.adapter.out.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class AuthConfigurationTest {

    @Test
    void requiresAccessTokenSecretFromEnvironment() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            properties.load(input);
        }

        assertEquals("${APP_AUTH_TOKEN_SECRET}", properties.getProperty("app.auth.token-secret"));
    }
}
