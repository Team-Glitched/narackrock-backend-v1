package glitched.adlips.application.user.account.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.account.dto.request.GoogleLoginRequest;
import glitched.adlips.application.user.account.dto.response.GoogleIdentityResponse;
import glitched.adlips.application.user.account.dto.response.GoogleLoginResponse;
import glitched.adlips.application.user.account.dto.response.GoogleLoginResponse.UserResponse;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.account.port.out.GoogleIdentityPort;
import glitched.adlips.application.user.account.port.out.UserAuthProviderRepositoryPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.MediaFileRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.media.MediaFile;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;

public class GoogleLoginUseCase {
    private final GoogleIdentityPort googleIdentityPort;
    private final AccessTokenPort accessTokenPort;
    private final UserRepositoryPort userRepository;
    private final UserAuthProviderRepositoryPort authProviderRepository;
    private final ProfileRepositoryPort profileRepository;
    private final MediaFileRepositoryPort mediaFileRepository;
    private final RefreshTokenManager refreshTokenManager;
    private final TransactionRunner transactionRunner;

    public GoogleLoginUseCase(
            GoogleIdentityPort googleIdentityPort,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository,
            UserAuthProviderRepositoryPort authProviderRepository,
            ProfileRepositoryPort profileRepository,
            RefreshTokenManager refreshTokenManager
    ) {
        this(
                googleIdentityPort,
                accessTokenPort,
                userRepository,
                authProviderRepository,
                profileRepository,
                null,
                refreshTokenManager,
                TransactionRunner.direct()
        );
    }

    public GoogleLoginUseCase(
            GoogleIdentityPort googleIdentityPort,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository,
            UserAuthProviderRepositoryPort authProviderRepository,
            ProfileRepositoryPort profileRepository,
            RefreshTokenManager refreshTokenManager,
            TransactionRunner transactionRunner
    ) {
        this(
                googleIdentityPort,
                accessTokenPort,
                userRepository,
                authProviderRepository,
                profileRepository,
                null,
                refreshTokenManager,
                transactionRunner
        );
    }

    public GoogleLoginUseCase(
            GoogleIdentityPort googleIdentityPort,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository,
            UserAuthProviderRepositoryPort authProviderRepository,
            ProfileRepositoryPort profileRepository,
            MediaFileRepositoryPort mediaFileRepository,
            RefreshTokenManager refreshTokenManager
    ) {
        this(
                googleIdentityPort,
                accessTokenPort,
                userRepository,
                authProviderRepository,
                profileRepository,
                mediaFileRepository,
                refreshTokenManager,
                TransactionRunner.direct()
        );
    }

    public GoogleLoginUseCase(
            GoogleIdentityPort googleIdentityPort,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository,
            UserAuthProviderRepositoryPort authProviderRepository,
            ProfileRepositoryPort profileRepository,
            MediaFileRepositoryPort mediaFileRepository,
            RefreshTokenManager refreshTokenManager,
            TransactionRunner transactionRunner
    ) {
        this.googleIdentityPort = googleIdentityPort;
        this.accessTokenPort = accessTokenPort;
        this.userRepository = userRepository;
        this.authProviderRepository = authProviderRepository;
        this.profileRepository = profileRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.refreshTokenManager = refreshTokenManager;
        this.transactionRunner = transactionRunner;
    }

    public GoogleLoginResponse execute(GoogleLoginRequest request) {
        return transactionRunner.readOnly(() -> executeInternal(request));
    }

    private GoogleLoginResponse executeInternal(GoogleLoginRequest request) {
        validateToken(request == null ? null : request.idToken());

        GoogleIdentityResponse identity = googleIdentityPort.verify(request.idToken());
        validateVerifiedEmail(identity);

        UserAuthProvider provider = authProviderRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, identity.subject())
                .orElseThrow(this::signupRequired);

        User user = userRepository.findById(provider.getUserId())
                .filter(User::isActive)
                .orElseThrow(this::signupRequired);

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.PROFILE_NOT_FOUND,
                        "사용자 프로필을 찾을 수 없습니다."
                ));

        return new GoogleLoginResponse(
                accessTokenPort.issue(user.getId()),
                refreshTokenManager.issue(user.getId()),
                "Bearer",
                new UserResponse(
                        user.getId(),
                        profile.getNickname(),
                        profileImageUrl(profile)
                )
        );
    }

    private String profileImageUrl(Profile profile) {
        if (mediaFileRepository == null || profile.getProfileImageFileId() == null) {
            return null;
        }

        return mediaFileRepository.findById(profile.getProfileImageFileId())
                .map(MediaFile::getFileUrl)
                .orElse(null);
    }

    private UserApplicationException signupRequired() {
        return new UserApplicationException(
                UserErrorCode.SIGNUP_REQUIRED,
                "가입되지 않은 계정입니다. 추가 정보 입력 페이지로 이동합니다."
        );
    }

    private void validateVerifiedEmail(GoogleIdentityResponse identity) {
        if (identity == null
                || identity.subject() == null
                || identity.subject().isBlank()
                || identity.email() == null
                || identity.email().isBlank()
                || !identity.emailVerified()) {
            throw new UserApplicationException(
                    UserErrorCode.AUTH_FAILED_GOOGLE,
                    "계정 인증을 실패했습니다."
            );
        }
    }

    private void validateToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UserApplicationException(
                    UserErrorCode.VALIDATION_ERROR,
                    "Google ID 토큰은 필수입니다."
            );
        }
    }
}