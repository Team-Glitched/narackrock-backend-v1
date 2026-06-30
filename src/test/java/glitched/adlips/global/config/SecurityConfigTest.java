package glitched.adlips.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @RestController
    public static class ProtectedController {

        @GetMapping("/protected")
        public String protectedEndpoint(java.security.Principal principal) {
            return principal.getName();
        }
    }
}
