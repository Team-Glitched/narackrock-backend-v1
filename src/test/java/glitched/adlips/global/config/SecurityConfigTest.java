package glitched.adlips.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.out.auth.HmacAccessTokenAdapter;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityConfigTest.ProtectedController.class)
@Import({
        SecurityConfig.class,
        SecurityConfigTest.ProtectedController.class,
        SecurityConfigTest.TokenConfiguration.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenPort accessTokenPort;

    @Test
    void allowsProtectedRequestWithIssuedBearerToken() throws Exception {
        String token = accessTokenPort.issue(42L);

        mockMvc.perform(get("/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    void rejectsProtectedRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    public static class ProtectedController {

        @GetMapping("/protected")
        public String protectedEndpoint(java.security.Principal principal) {
            return principal.getName();
        }
    }

    @TestConfiguration
    static class TokenConfiguration {

        @Bean
        AccessTokenPort accessTokenPort() {
            return new HmacAccessTokenAdapter(
                    "test-secret-key-at-least-32-characters-long!!",
                    3_600L,
                    Clock.systemUTC()
            );
        }
    }
}
