package glitched.adlips.adapter.out.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenIssuerAdapterTest {

    private static final String SECRET = "test-secret-key-at-least-32-characters-long!!";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtTokenIssuerAdapter issuer;

    @BeforeEach
    void setUp() {
        issuer = new JwtTokenIssuerAdapter(SECRET, EXPIRATION_MS);
    }

    @Test
    void issuedTokenContainsUserId() {
        String token = issuer.issue(42L);

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("42");
    }

    @Test
    void issuedTokenIsNotExpiredImmediately() {
        String token = issuer.issue(1L);

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void differentUserIdsProduceDifferentTokens() {
        String token1 = issuer.issue(1L);
        String token2 = issuer.issue(2L);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void verifiesIssuedTokenAndReturnsUserId() {
        String token = issuer.issue(42L);

        assertThat(issuer.verifyAndExtractUserId(token)).isEqualTo(42L);
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtTokenIssuerAdapter anotherIssuer = new JwtTokenIssuerAdapter(
                "another-test-secret-at-least-32-characters!!",
                EXPIRATION_MS
        );
        String token = anotherIssuer.issue(42L);

        assertThatThrownBy(() -> issuer.verifyAndExtractUserId(token))
                .isInstanceOf(JwtException.class);
    }
}
