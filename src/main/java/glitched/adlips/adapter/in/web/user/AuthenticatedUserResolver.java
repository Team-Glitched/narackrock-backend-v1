package glitched.adlips.adapter.in.web.user;

import glitched.adlips.application.user.UserApplicationException;
import glitched.adlips.application.user.UserErrorCode;
import glitched.adlips.application.user.port.out.AccessTokenPort;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenPort accessTokenPort;

    public AuthenticatedUserResolver(AccessTokenPort accessTokenPort) {
        this.accessTokenPort = accessTokenPort;
    }

    public Long requireUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw unauthorized();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw unauthorized();
        }
        return accessTokenPort.verify(token);
    }

    private UserApplicationException unauthorized() {
        return new UserApplicationException(
                UserErrorCode.UNAUTHORIZED_ACCESS,
                "인증 정보가 없거나 만료되었습니다. 로그인을 다시 진행해 주세요."
        );
    }
}
