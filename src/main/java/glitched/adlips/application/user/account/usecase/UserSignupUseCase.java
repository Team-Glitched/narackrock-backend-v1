package glitched.adlips.application.user.account.usecase;

import glitched.adlips.application.user.account.dto.request.UserSignupRequest;
import glitched.adlips.application.user.account.dto.response.GoogleIdentityResponse;
import glitched.adlips.application.user.account.dto.response.UserSignupResponse;
import glitched.adlips.application.user.account.dto.response.UserSignupResponse.UserResponse;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.account.port.out.GoogleIdentityPort;
import glitched.adlips.application.user.account.port.out.UserAuthProviderRepositoryPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSignupUseCase {
    private final GoogleIdentityPort googleIdentityPort;
    private final AccessTokenPort accessTokenPort;
    private final UserRepositoryPort userRepository;
    private final UserAuthProviderRepositoryPort authProviderRepository;
    private final ProfileRepositoryPort profileRepository;

    public UserSignupUseCase(
            GoogleIdentityPort googleIdentityPort,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository,
            UserAuthProviderRepositoryPort authProviderRepository,
            ProfileRepositoryPort profileRepository
    ) {
        this.googleIdentityPort = googleIdentityPort;
        this.accessTokenPort = accessTokenPort;
        this.userRepository = userRepository;
        this.authProviderRepository = authProviderRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public UserSignupResponse execute(UserSignupRequest request) {
        validateToken(request == null ? null : request.idToken());
        String nickname = validateNickname(request.nickname());
        GoogleIdentityResponse identity = googleIdentityPort.verify(request.idToken());
        validateVerifiedEmail(identity);

        if (authProviderRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, identity.subject())
                .isPresent()) {
            throw new UserApplicationException(UserErrorCode.ALREADY_REGISTERED, "이미 가입된 계정입니다.");
        }
        if (profileRepository.existsByNickname(nickname)) {
            throw new UserApplicationException(UserErrorCode.DUPLICATE_NICKNAME, "이미 사용중인 닉네임입니다.");
        }

        User user = userRepository.findByEmail(identity.email())
                .filter(User::isActive)
                .orElseGet(() -> userRepository.save(User.create(identity.email())));
        authProviderRepository.save(UserAuthProvider.google(
                user.getId(), identity.subject(), identity.emailVerified()
        ));
        Profile profile = profileRepository.save(Profile.create(user.getId(), nickname));
        return new UserSignupResponse(
                accessTokenPort.issue(user.getId()),
                "Bearer",
                new UserResponse(user.getId(), profile.getNickname(), null)
        );
    }

    private void validateVerifiedEmail(GoogleIdentityResponse identity) {
        if (identity == null || identity.subject() == null || identity.subject().isBlank()
                || identity.email() == null || identity.email().isBlank() || !identity.emailVerified()) {
            throw new UserApplicationException(UserErrorCode.AUTH_FAILED_GOOGLE, "계정 인증을 실패했습니다.");
        }
    }

    private void validateToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "Google ID 토큰은 필수입니다.");
        }
    }

    private String validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "닉네임이 입력되지 않았습니다.");
        }
        return nickname.trim();
    }
}
