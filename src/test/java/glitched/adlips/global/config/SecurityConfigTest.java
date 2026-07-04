package glitched.adlips.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityConfigTest.ProtectedController.class)
@Import({SecurityConfig.class, SecurityConfigTest.ProtectedController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenIssuerAdapter tokenAdapter;

    @Test
    void allowsProtectedRequestWithIssuedBearerToken() throws Exception {
        String token = tokenAdapter.issue(42L);

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

    @RestController
    public static class ProtectedController {

        @GetMapping("/protected")
        public String protectedEndpoint(java.security.Principal principal) {
            return principal.getName();
        }
    }
}
