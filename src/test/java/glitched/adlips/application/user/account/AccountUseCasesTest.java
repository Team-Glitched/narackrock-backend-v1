package glitched.adlips.application.user.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glitched.adlips.application.user.account.dto.request.GoogleLoginRequest;
import glitched.adlips.application.user.account.dto.request.UserSignupRequest;
import glitched.adlips.application.user.account.dto.request.UserWithdrawRequest;
import glitched.adlips.application.user.account.dto.response.GoogleIdentityResponse;
import glitched.adlips.application.user.account.dto.response.GoogleLoginResponse;
import glitched.adlips.application.user.account.dto.response.UserSignupResponse;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.account.port.out.GoogleIdentityPort;
import glitched.adlips.application.user.account.port.out.UserAuthProviderRepositoryPort;
import glitched.adlips.application.user.account.usecase.GoogleLoginUseCase;
import glitched.adlips.application.user.account.usecase.RefreshTokenManager;
import glitched.adlips.application.user.account.usecase.UserSignupUseCase;
import glitched.adlips.application.user.account.usecase.UserWithdrawUseCase;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.media.MediaFileStatus;
import glitched.adlips.domain.media.MediaFileType;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountUseCasesTest {

    private InMemoryUserRepository users;
    private InMemoryAuthProviderRepository authProviders;
    private InMemoryProfileRepository profiles;
    private InMemoryMediaRepository media;
    private UserSignupUseCase userSignupUseCase;
    private GoogleLoginUseCase googleLoginUseCase;
    private UserWithdrawUseCase userWithdrawUseCase;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        authProviders = new InMemoryAuthProviderRepository();
        profiles = new InMemoryProfileRepository();
        media = new InMemoryMediaRepository();
        GoogleIdentityPort google = token -> new GoogleIdentityResponse("google-sub", "user@example.com", true);
        AccessTokenPort accessTokens = new AccessTokenPort() {
            @Override
            public String issue(Long userId) {
                return "access-" + userId;
            }

            @Override
            public Long verify(String token) {
                return Long.valueOf(token.substring("access-".length()));
            }
        };
        RefreshTokenManager refreshTokens = mock(RefreshTokenManager.class);
        when(refreshTokens.issue(anyLong())).thenReturn("refresh-1");
        Clock clock = Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC);
        userSignupUseCase = new UserSignupUseCase(
                google, accessTokens, users, authProviders, profiles, refreshTokens
        );
        googleLoginUseCase = new GoogleLoginUseCase(
                google, accessTokens, users, authProviders, profiles, media, refreshTokens
        );
        userWithdrawUseCase = new UserWithdrawUseCase(users, refreshTokens, clock);
    }

    @Test
    void signsUpGoogleUserAndCreatesProfile() {
        UserSignupResponse result = userSignupUseCase.execute(
                new UserSignupRequest("valid-token", "guitar_moon")
        );

        assertEquals("access-1", result.accessToken());
        assertEquals("refresh-1", result.refreshToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(1L, result.user().userId());
        assertEquals("guitar_moon", result.user().nickname());
        assertNotNull(users.findById(1L).orElseThrow());
        assertNotNull(profiles.findByUserId(1L).orElseThrow());
        assertTrue(authProviders.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub").isPresent());
    }

    @Test
    void rejectsDuplicatedNickname() {
        User existing = users.save(User.create("existing@example.com"));
        profiles.save(Profile.create(existing.getId(), "guitar_moon"));

        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> userSignupUseCase.execute(new UserSignupRequest("valid-token", "guitar_moon"))
        );

        assertEquals(UserErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
    }

    @Test
    void requiresSignupWhenGoogleAccountIsUnknown() {
        UserApplicationException exception = assertThrows(
                UserApplicationException.class,
                () -> googleLoginUseCase.execute(new GoogleLoginRequest("valid-token"))
        );

        assertEquals(UserErrorCode.SIGNUP_REQUIRED, exception.getErrorCode());
    }

    @Test
    void logsInRegisteredGoogleUser() {
        User user = users.save(User.create("user@example.com"));
        profiles.save(Profile.create(user.getId(), "guitar_moon"));
        authProviders.save(UserAuthProvider.google(user.getId(), "google-sub", true));

        GoogleLoginResponse result = googleLoginUseCase.execute(new GoogleLoginRequest("valid-token"));

        assertEquals("access-1", result.accessToken());
        assertEquals("refresh-1", result.refreshToken());
        assertEquals("guitar_moon", result.user().nickname());
    }

    @Test
    void returnsProfileImageUrlWhenRegisteredGoogleUserHasImage() {
        User user = users.save(User.create("user@example.com"));
        profiles.save(Profile.restore(user.getId(), "guitar_moon", 10L, null, null, false, 0, 0, null));
        media.save(MediaFile.restore(
                10L,
                user.getId(),
                "https://cdn.test/profile.png",
                "profile",
                "profile.png",
                MediaFileType.IMAGE,
                "image/png",
                1L,
                MediaFileStatus.READY
        ));
        authProviders.save(UserAuthProvider.google(user.getId(), "google-sub", true));

        GoogleLoginResponse result = googleLoginUseCase.execute(new GoogleLoginRequest("valid-token"));

        assertEquals("https://cdn.test/profile.png", result.user().profileImageUrl());
    }

    @Test
    void withdrawsAuthenticatedUser() {
        User user = users.save(User.create("user@example.com"));

        userWithdrawUseCase.execute(new UserWithdrawRequest(user.getId()));

        User withdrawn = users.findById(user.getId()).orElseThrow();
        assertEquals(LocalDateTime.of(2026, 6, 30, 0, 0), withdrawn.getDeletedAt());
    }

    private static final class InMemoryUserRepository implements UserRepositoryPort {
        private final Map<Long, User> values = new HashMap<>();
        private long sequence = 1;

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return values.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
        }

        @Override
        public User save(User user) {
            User saved = user.getId() == null ? user.withId(sequence++) : user;
            values.put(saved.getId(), saved);
            return saved;
        }
    }

    private static final class InMemoryAuthProviderRepository implements UserAuthProviderRepositoryPort {
        private final Map<String, UserAuthProvider> values = new HashMap<>();

        @Override
        public Optional<UserAuthProvider> findByProviderAndProviderUserId(
                AuthProvider provider,
                String providerUserId
        ) {
            return Optional.ofNullable(values.get(provider + ":" + providerUserId));
        }

        @Override
        public UserAuthProvider save(UserAuthProvider authProvider) {
            values.put(authProvider.getProvider() + ":" + authProvider.getProviderUserId(), authProvider);
            return authProvider;
        }
    }

    private static final class InMemoryProfileRepository implements ProfileRepositoryPort {
        private final Map<Long, Profile> values = new HashMap<>();

        @Override
        public Optional<Profile> findByUserId(Long userId) {
            return Optional.ofNullable(values.get(userId));
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return values.values().stream().anyMatch(profile -> profile.getNickname().equals(nickname));
        }

        @Override
        public boolean existsByNicknameAndUserIdNot(String nickname, Long userId) {
            return values.values().stream().anyMatch(
                    profile -> !profile.getUserId().equals(userId) && profile.getNickname().equals(nickname)
            );
        }

        @Override
        public Profile save(Profile profile) {
            values.put(profile.getUserId(), profile);
            return profile;
        }
    }

    private static final class InMemoryMediaRepository implements MediaFileRepositoryPort {
        private final Map<Long, MediaFile> values = new HashMap<>();

        @Override
        public Optional<MediaFile> findById(Long id) {
            return Optional.ofNullable(values.get(id));
        }

        @Override
        public MediaFile save(MediaFile file) {
            values.put(file.getId(), file);
            return file;
        }
    }
}
