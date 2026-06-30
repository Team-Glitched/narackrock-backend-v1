package glitched.adlips.adapter.out.auth;

import glitched.adlips.application.user.UserApplicationException;
import glitched.adlips.application.user.UserErrorCode;
import glitched.adlips.application.user.port.out.AccessTokenPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HmacAccessTokenAdapter implements AccessTokenPort {
    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;
    private final long validitySeconds;
    private final Clock clock;

    public HmacAccessTokenAdapter(
            @Value("${app.auth.token-secret}") String secret,
            @Value("${app.auth.token-validity-seconds}") long validitySeconds,
            Clock clock
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.validitySeconds = validitySeconds;
        this.clock = clock;
    }

    @Override
    public String issue(Long userId) {
        long expiresAt = Instant.now(clock).plusSeconds(validitySeconds).getEpochSecond();
        String payload = userId + ":" + expiresAt;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sign(encodedPayload);
    }

    @Override
    public Long verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2) {
                throw unauthorized();
            }
            byte[] expected = sign(parts[0]).getBytes(StandardCharsets.US_ASCII);
            byte[] actual = parts[1].getBytes(StandardCharsets.US_ASCII);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw unauthorized();
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] values = payload.split(":", -1);
            if (values.length != 2 || Instant.now(clock).getEpochSecond() >= Long.parseLong(values[1])) {
                throw unauthorized();
            }
            return Long.valueOf(values[0]);
        } catch (IllegalArgumentException exception) {
            throw unauthorized();
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("접근 토큰 서명에 실패했습니다.", exception);
        }
    }

    private UserApplicationException unauthorized() {
        return new UserApplicationException(
                UserErrorCode.UNAUTHORIZED_ACCESS,
                "인증 정보가 없거나 만료되었습니다. 로그인을 다시 진행해 주세요."
        );
    }
}
