package glitched.adlips.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AuthConfigTest {

    @Test
    void configuresExpectedGoogleAudience() {
        AuthConfig config = new AuthConfig();

        assertThat(config.googleIdTokenVerifier("google-client-id").getAudience())
                .containsExactly("google-client-id");
    }

    @Test
    void rejectsBlankGoogleClientId() {
        AuthConfig config = new AuthConfig();

        assertThatThrownBy(() -> config.googleIdTokenVerifier(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
