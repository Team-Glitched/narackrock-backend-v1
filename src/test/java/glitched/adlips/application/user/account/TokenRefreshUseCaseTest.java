package glitched.adlips.application.user.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.user.account.dto.request.TokenRefreshRequest;
import glitched.adlips.application.user.account.dto.response.TokenRefreshResponse;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.account.usecase.RefreshTokenManager;
import glitched.adlips.application.user.account.usecase.TokenRefreshUseCase;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TokenRefreshUseCaseTest {

    @Test
    void returnsNewAccessAndRotatedRefreshToken() {
        RefreshTokenManager refreshTokenManager = mock(RefreshTokenManager.class);
        when(refreshTokenManager.rotate("old-refresh"))
                .thenReturn(new RefreshTokenManager.Rotation(1L, "new-refresh"));
        AccessTokenPort accessTokenPort = new AccessTokenPort() {
            @Override
            public String issue(Long userId) {
                return "new-access";
            }

            @Override
            public Long verify(String token) {
                return 1L;
            }
        };
        UserRepositoryPort users = new UserRepositoryPort() {
            @Override
            public Optional<User> findById(Long id) {
                return Optional.of(User.create("user@example.com").withId(id));
            }

            @Override
            public Optional<User> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public User save(User user) {
                return user;
            }
        };
        TokenRefreshUseCase useCase = new TokenRefreshUseCase(refreshTokenManager, accessTokenPort, users);

        TokenRefreshResponse response = useCase.execute(new TokenRefreshRequest("old-refresh"));

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
    }
}
