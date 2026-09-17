package glitched.adlips.adapter.in.security;

import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.test-auth.enabled", havingValue = "true")
public class LocalTestAuthenticationFilter extends OncePerRequestFilter {
    private final AccessTokenPort tokens;
    private final Long userId;

    public LocalTestAuthenticationFilter(
            AccessTokenPort tokens,
            @Value("${app.test-auth.user-id:1}") Long userId,
            @Value("${spring.datasource.url}") String datasourceUrl
    ) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("테스트 사용자 ID가 필요합니다.");
        if (datasourceUrl == null || !(datasourceUrl.startsWith("jdbc:h2:mem:")
                || datasourceUrl.startsWith("jdbc:h2:file:")
                || datasourceUrl.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1|\\[::1\\]):[0-9]+/.*"))) {
            throw new IllegalStateException("테스트 인증 우회는 로컬 데이터베이스에서만 사용할 수 있습니다.");
        }
        this.tokens = tokens;
        this.userId = userId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getHeader("Authorization") != null || !isLoopback(request.getRemoteAddr())) {
            chain.doFilter(request, response);
            return;
        }
        String authorization = "Bearer " + tokens.issue(userId);
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return "Authorization".equalsIgnoreCase(name) ? authorization : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return "Authorization".equalsIgnoreCase(name)
                        ? Collections.enumeration(List.of(authorization)) : super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                if (names.stream().noneMatch(name -> "Authorization".equalsIgnoreCase(name))) {
                    names = new ArrayList<>(names);
                    names.add("Authorization");
                }
                return Collections.enumeration(names);
            }
        }, response);
    }

    private boolean isLoopback(String remoteAddress) {
        try {
            return InetAddress.getByName(remoteAddress).isLoopbackAddress();
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
