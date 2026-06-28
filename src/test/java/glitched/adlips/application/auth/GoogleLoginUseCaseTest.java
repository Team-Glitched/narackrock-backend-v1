package glitched.adlips.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import glitched.adlips.application.exception.AuthFailedException;
import glitched.adlips.application.exception.SignupRequiredException;
import glitched.adlips.application.port.GoogleTokenVerifier;
import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.application.port.TokenIssuer;
import glitched.adlips.application.port.UserAuthProviderRepository;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleLoginUseCaseTest {

    @Mock private GoogleTokenVerifier tokenVerifier;
    @Mock private UserAuthProviderRepository authProviderRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private TokenIssuer tokenIssuer;

    private GoogleLoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GoogleLoginUseCase(tokenVerifier, authProviderRepository, profileRepository, tokenIssuer);
    }

    @Test
    void loginSuccessfully() {
        User user = new User("user@gmail.com");
        UserAuthProvider provider = new UserAuthProvider(user, AuthProvider.GOOGLE, "google-sub-123", true);
        Profile profile = new Profile(user, "guitar_moon");

        given(tokenVerifier.verify("valid-token")).willReturn(new GoogleUserInfo("google-sub-123", "user@gmail.com", true));
        given(authProviderRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub-123"))
                .willReturn(Optional.of(provider));
        given(profileRepository.findByUserId(any())).willReturn(Optional.of(profile));
        given(tokenIssuer.issue(any())).willReturn("jwt-token");

        AuthResult result = useCase.login("valid-token");

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.nickname()).isEqualTo("guitar_moon");
        assertThat(result.profileImageUrl()).isNull();
    }

    @Test
    void throwsAuthFailedWhenGoogleTokenInvalid() {
        given(tokenVerifier.verify(anyString())).willThrow(new AuthFailedException());

        assertThatThrownBy(() -> useCase.login("invalid-token"))
                .isInstanceOf(AuthFailedException.class);
    }

    @Test
    void throwsSignupRequiredWhenUserNotRegistered() {
        given(tokenVerifier.verify("valid-token")).willReturn(new GoogleUserInfo("google-sub-123", "user@gmail.com", true));
        given(authProviderRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub-123"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.login("valid-token"))
                .isInstanceOf(SignupRequiredException.class);
    }

    @Test
    void throwsSignupRequiredWhenProfileNotFound() {
        User user = new User("user@gmail.com");
        UserAuthProvider provider = new UserAuthProvider(user, AuthProvider.GOOGLE, "google-sub-123", true);

        given(tokenVerifier.verify("valid-token")).willReturn(new GoogleUserInfo("google-sub-123", "user@gmail.com", true));
        given(authProviderRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub-123"))
                .willReturn(Optional.of(provider));
        given(profileRepository.findByUserId(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.login("valid-token"))
                .isInstanceOf(SignupRequiredException.class);
    }
}
