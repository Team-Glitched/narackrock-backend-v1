package glitched.adlips.global.config;

import glitched.adlips.adapter.in.security.AccessTokenAuthenticationFilter;
import glitched.adlips.adapter.in.security.JwtAuthenticationFilter;
import glitched.adlips.adapter.out.token.JwtTokenIssuerAdapter;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AccessTokenPort accessTokenPort,
            JwtTokenIssuerAdapter jwtTokenAdapter,
            @Value("${app.legacy-auth.enabled:false}") boolean legacyAuthEnabled
    ) throws Exception {
        OncePerRequestFilter authenticationFilter = legacyAuthEnabled
                ? new JwtAuthenticationFilter(jwtTokenAdapter)
                : new AccessTokenAuthenticationFilter(accessTokenPort);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/api/v1/users").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/shorts").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )
                .addFilterBefore(
                        authenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }

    @Bean
    public JwtTokenIssuerAdapter jwtTokenIssuerAdapter(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        return new JwtTokenIssuerAdapter(secret, expirationMs);
    }
}
