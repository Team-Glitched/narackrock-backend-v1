package glitched.adlips.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import glitched.adlips.application.exception.AlreadyRegisteredException;
import glitched.adlips.application.exception.AuthFailedException;
import glitched.adlips.application.exception.DuplicateNicknameException;
import glitched.adlips.application.port.GoogleTokenVerifier;
import glitched.adlips.application.port.ProfileRepository;
import glitched.adlips.application.port.TokenIssuer;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.port.UserAuthProviderRepository;
import glitched.adlips.application.port.UserRepository;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignUpWithGoogleUseCaseTest {

    @Mock private GoogleTokenVerifier tokenVerifier;
    @Mock private UserRepository userRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private UserAuthProviderRepository authProviderRepository;
    @Mock private TokenIssuer tokenIssuer;
    @Mock private TransactionRunner transactionRunner;

    private SignUpWithGoogleUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
                .when(transactionRunner).required(any());
        useCase = new SignUpWithGoogleUseCase(
                tokenVerifier,
                userRepository,
                profileRepository,
                authProviderRepository,
                tokenIssuer,
                transactionRunner
        );
    }

    @Test
    void signUpSuccessfully() {
        User savedUser = new User("user@gmail.com");
        Profile savedProfile = new Profile(savedUser, "guitar_moon");

        given(tokenVerifier.verify("valid-token")).willReturn(new GoogleUserInfo("google-sub-123", "user@gmail.com", true));
        given(authProviderRepository.existsByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub-123")).willReturn(false);
        given(profileRepository.existsByNickname("guitar_moon")).willReturn(false);
        given(userRepository.save(any())).willReturn(savedUser);
        given(profileRepository.save(any())).willReturn(savedProfile);
        given(authProviderRepository.save(any())).willReturn(null);
        given(tokenIssuer.issue(any())).willReturn("jwt-token");

        AuthResult result = useCase.signUp("valid-token", "guitar_moon");

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.nickname()).isEqualTo("guitar_moon");
        verify(transactionRunner).required(any());
    }

    @Test
    void throwsAuthFailedWhenGoogleTokenInvalid() {
        given(tokenVerifier.verify("invalid-token")).willThrow(new AuthFailedException());

        assertThatThrownBy(() -> useCase.signUp("invalid-token", "guitar_moon"))
                .isInstanceOf(AuthFailedException.class);
    }

    @Test
    void throwsAlreadyRegisteredWhenGoogleAccountExists() {
        given(tokenVerifier.verify("valid-token")).willReturn(new GoogleUserInfo("google-sub-123", "user@gmail.com", true));
        given(authProviderRepository.existsByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub-123")).willReturn(true);

        assertThatThrownBy(() -> useCase.signUp("valid-token", "guitar_moon"))
                .isInstanceOf(AlreadyRegisteredException.class);
    }

    @Test
    void throwsDuplicateNicknameWhenNicknameAlreadyInUse() {
        given(tokenVerifier.verify("valid-token")).willReturn(new GoogleUserInfo("google-sub-123", "user@gmail.com", true));
        given(authProviderRepository.existsByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub-123")).willReturn(false);
        given(profileRepository.existsByNickname("taken_name")).willReturn(true);

        assertThatThrownBy(() -> useCase.signUp("valid-token", "taken_name"))
                .isInstanceOf(DuplicateNicknameException.class);
    }

    @Test
    void throwsAlreadyRegisteredWhenEmailAccountExists() {
        User existingUser = new User("user@gmail.com");
        given(tokenVerifier.verify("valid-token"))
                .willReturn(new GoogleUserInfo("google-sub-123", "user@gmail.com", true));
        given(authProviderRepository.existsByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub-123"))
                .willReturn(false);
        given(userRepository.findByEmail("user@gmail.com")).willReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> useCase.signUp("valid-token", "guitar_moon"))
                .isInstanceOf(AlreadyRegisteredException.class);
    }
}
