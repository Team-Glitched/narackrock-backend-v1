package glitched.adlips.adapter.in.web.user;

import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenPort accessTokenPort;
    private final UserRepositoryPort userRepository;

    public AuthenticatedUserResolver(
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository
    ) {
        this.accessTokenPort = accessTokenPort;
        this.userRepository = userRepository;
    }

    public Long requireUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw unauthorized();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw unauthorized();
        }
        Long userId = accessTokenPort.verify(token);
        return userRepository.findById(userId)
                .filter(user -> user.isActive())
                .map(user -> userId)
                .orElseThrow(this::unauthorized);
    }

    private UserApplicationException unauthorized() {
        return new UserApplicationException(
                UserErrorCode.UNAUTHORIZED_ACCESS,
                "인증 정보가 없거나 만료되었습니다. 로그인을 다시 진행해 주세요."
        );
    }
}
