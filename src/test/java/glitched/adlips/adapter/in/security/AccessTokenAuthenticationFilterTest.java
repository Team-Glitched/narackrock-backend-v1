package glitched.adlips.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;

import glitched.adlips.adapter.out.auth.HmacAccessTokenAdapter;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class AccessTokenAuthenticationFilterTest {

    private AccessTokenPort accessTokenPort;
    private AccessTokenAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        accessTokenPort = new HmacAccessTokenAdapter(
                "test-secret-key-at-least-32-characters-long!!",
                3_600L,
                Clock.systemUTC()
        );
        filter = new AccessTokenAuthenticationFilter(accessTokenPort);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 실제_발급_액세스_토큰으로_요청을_인증한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessTokenPort.issue(42L));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> continued.set(true);

        filter.doFilter(request, response, chain);

        assertThat(continued).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(42L);
    }

    @Test
    void 잘못된_액세스_토큰은_인증하지_않는다() throws Exception {
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
