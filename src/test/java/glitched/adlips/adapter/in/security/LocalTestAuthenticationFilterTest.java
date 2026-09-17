package glitched.adlips.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LocalTestAuthenticationFilterTest {
    @Test
    void addsBearerTokenOnlyForLoopbackRequestsWithoutAuthorization() throws Exception {
        AccessTokenPort tokens = mock(AccessTokenPort.class);
        when(tokens.issue(1L)).thenReturn("local-test-token");
        LocalTestAuthenticationFilter filter = new LocalTestAuthenticationFilter(tokens, 1L, "jdbc:postgresql://localhost:5432/adlips_test");
        MockHttpServletRequest local = new MockHttpServletRequest("GET", "/api/v1/projects/1/tracks");
        local.setRemoteAddr("127.0.0.1");
        MockFilterChain localChain = new MockFilterChain();

        filter.doFilter(local, new MockHttpServletResponse(), localChain);

        assertThat(((HttpServletRequest) localChain.getRequest()).getHeader("Authorization"))
                .isEqualTo("Bearer local-test-token");

        MockHttpServletRequest remote = new MockHttpServletRequest("GET", "/api/v1/projects/1/tracks");
        remote.setRemoteAddr("203.0.113.10");
        MockFilterChain remoteChain = new MockFilterChain();
        filter.doFilter(remote, new MockHttpServletResponse(), remoteChain);
        assertThat(((HttpServletRequest) remoteChain.getRequest()).getHeader("Authorization")).isNull();
    }

    @Test
    void rejectsRemoteDatabaseWhenTestAuthIsEnabled() {
        assertThatThrownBy(() -> new LocalTestAuthenticationFilter(mock(AccessTokenPort.class), 1L,
                "jdbc:postgresql://aws-1-ap-northeast-2.pooler.supabase.com:5432/postgres"))
                .isInstanceOf(IllegalStateException.class);
    }
}
