package glitched.adlips.adapter.in.web.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserResolverTest {
    @Mock
    private AccessTokenPort accessTokenPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    private AuthenticatedUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuthenticatedUserResolver(accessTokenPort, userRepositoryPort);
    }

    @Test
    void resolvesMainAccessTokenForOptionalAuthentication() {
        given(accessTokenPort.verify("main-token")).willReturn(99L);
        given(userRepositoryPort.findById(99L)).willReturn(Optional.of(
                User.restore(99L, "viewer@test.com", UserRole.USER, null)));

        assertThat(resolver.resolveOptionalUserId("Bearer main-token")).isEqualTo(99L);
    }

    @Test
    void treatsMissingOrInvalidOptionalAuthenticationAsGuest() {
        given(accessTokenPort.verify("invalid-token")).willThrow(new UserApplicationException(
                UserErrorCode.UNAUTHORIZED_ACCESS, "인증 실패"));

        assertThat(resolver.resolveOptionalUserId(null)).isNull();
        assertThat(resolver.resolveOptionalUserId("Bearer invalid-token")).isNull();
    }
}
