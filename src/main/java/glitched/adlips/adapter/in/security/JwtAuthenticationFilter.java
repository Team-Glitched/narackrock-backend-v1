package glitched.adlips.adapter.in.security;

import glitched.adlips.application.port.AccessTokenVerifier;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenVerifier tokenVerifier;

    public JwtAuthenticationFilter(AccessTokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            authenticate(authorization.substring(BEARER_PREFIX.length()));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Long userId = tokenVerifier.verifyAndExtractUserId(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
