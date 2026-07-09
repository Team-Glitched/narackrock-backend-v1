package glitched.adlips.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
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

    @Test
    void allowsConfiguredFrontendCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/media/upload-sessions")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void allowsLocalFileGetWithoutToken() throws Exception {
        mockMvc.perform(get("/files/missing.wav"))
                .andExpect(status().isNotFound());
    }

    @Test
    void allowsUserRelationsGetWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/relations?type=following"))
                .andExpect(status().isOk())
                .andExpect(content().string("relations"));
    }

    @RestController
    public static class ProtectedController {

        @GetMapping("/protected")
        public String protectedEndpoint(java.security.Principal principal) {
            return principal.getName();
        }

        @GetMapping("/api/v1/users/{userId}/relations")
        public String relations() {
            return "relations";
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
