package glitched.adlips.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private JwtTokenIssuerAdapter tokenAdapter;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        tokenAdapter = new JwtTokenIssuerAdapter(
                "test-secret-key-at-least-32-characters-long!!",
                3_600_000L
        );
        filter = new JwtAuthenticationFilter(tokenAdapter);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesRequestWithValidBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenAdapter.issue(42L));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> continued.set(true);

        filter.doFilter(request, response, chain);

        assertThat(continued).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(42L);
    }

    @Test
    void leavesInvalidBearerTokenUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> continued.set(true);

        filter.doFilter(request, response, chain);

        assertThat(continued).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
