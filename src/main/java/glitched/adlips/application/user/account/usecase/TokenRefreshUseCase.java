package glitched.adlips.application.user.account.usecase;

import glitched.adlips.application.user.account.dto.request.TokenRefreshRequest;
import glitched.adlips.application.user.account.dto.response.TokenRefreshResponse;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.port.TransactionRunner;

public class TokenRefreshUseCase {
    private final RefreshTokenManager refreshTokenManager;
    private final AccessTokenPort accessTokenPort;
    private final UserRepositoryPort userRepository;
    private final TransactionRunner transactionRunner;

    public TokenRefreshUseCase(
            RefreshTokenManager refreshTokenManager,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository
    ) {
        this(refreshTokenManager, accessTokenPort, userRepository, TransactionRunner.direct());
    }

    public TokenRefreshUseCase(
            RefreshTokenManager refreshTokenManager,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository,
            TransactionRunner transactionRunner
    ) {
        this.refreshTokenManager = refreshTokenManager;
        this.accessTokenPort = accessTokenPort;
        this.userRepository = userRepository;
        this.transactionRunner = transactionRunner;
    }

    public TokenRefreshResponse execute(TokenRefreshRequest request) {
        return transactionRunner.required(() -> executeInternal(request));
    }

    private TokenRefreshResponse executeInternal(TokenRefreshRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw invalidRefreshToken();
        }
        RefreshTokenManager.Rotation rotation = refreshTokenManager.rotate(request.refreshToken());
        userRepository.findById(rotation.userId())
                .filter(user -> user.isActive())
                .orElseThrow(this::invalidRefreshToken);
        return new TokenRefreshResponse(
                accessTokenPort.issue(rotation.userId()),
                rotation.refreshToken(),
                "Bearer"
        );
    }

    private UserApplicationException invalidRefreshToken() {
        return new UserApplicationException(
                UserErrorCode.INVALID_REFRESH_TOKEN,
                "유효하지 않거나 만료된 refresh token입니다."
        );
    }
}
