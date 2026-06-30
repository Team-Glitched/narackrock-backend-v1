package glitched.adlips.application.user.account;

import glitched.adlips.application.user.account.AuthResult.AuthenticatedUser;
import glitched.adlips.application.user.account.port.out.AccessTokenPort;
import glitched.adlips.application.user.account.port.out.GoogleIdentityPort;
import glitched.adlips.application.user.account.port.out.UserAuthProviderRepositoryPort;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.TransactionPort;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.AuthProvider;
import glitched.adlips.domain.user.Profile;
import glitched.adlips.domain.user.User;
import glitched.adlips.domain.user.UserAuthProvider;
import java.time.Clock;
import java.time.LocalDateTime;

public final class AccountService {
    private final GoogleIdentityPort googleIdentityPort;
    private final AccessTokenPort accessTokenPort;
    private final UserRepositoryPort userRepository;
    private final UserAuthProviderRepositoryPort authProviderRepository;
    private final ProfileRepositoryPort profileRepository;
    private final TransactionPort transactionPort;
    private final Clock clock;

    public AccountService(
            GoogleIdentityPort googleIdentityPort,
            AccessTokenPort accessTokenPort,
            UserRepositoryPort userRepository,
            UserAuthProviderRepositoryPort authProviderRepository,
            ProfileRepositoryPort profileRepository,
            TransactionPort transactionPort,
            Clock clock
    ) {
        this.googleIdentityPort = googleIdentityPort;
        this.accessTokenPort = accessTokenPort;
        this.userRepository = userRepository;
        this.authProviderRepository = authProviderRepository;
        this.profileRepository = profileRepository;
        this.transactionPort = transactionPort;
        this.clock = clock;
    }

    public AuthResult signup(SignupCommand command) {
        validateToken(command == null ? null : command.idToken());
        String nickname = validateNickname(command.nickname());
        GoogleIdentity identity = googleIdentityPort.verify(command.idToken());
        validateVerifiedEmail(identity);

        return transactionPort.required(() -> {
            if (authProviderRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, identity.subject())
                    .isPresent()) {
                throw new UserApplicationException(
                        UserErrorCode.ALREADY_REGISTERED,
                        "이미 가입된 계정입니다."
                );
            }
            if (profileRepository.existsByNickname(nickname)) {
                throw new UserApplicationException(
                        UserErrorCode.DUPLICATE_NICKNAME,
                        "이미 사용중인 닉네임입니다."
                );
            }

            User user = userRepository.findByEmail(identity.email())
                    .filter(User::isActive)
                    .orElseGet(() -> userRepository.save(User.create(identity.email())));
            authProviderRepository.save(UserAuthProvider.google(
                    user.getId(), identity.subject(), identity.emailVerified()
            ));
            Profile profile = profileRepository.save(Profile.create(user.getId(), nickname));
            return authResult(user, profile);
        });
    }

    public AuthResult login(LoginCommand command) {
        validateToken(command == null ? null : command.idToken());
        GoogleIdentity identity = googleIdentityPort.verify(command.idToken());
        validateVerifiedEmail(identity);

        UserAuthProvider provider = authProviderRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, identity.subject())
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.SIGNUP_REQUIRED,
                        "가입되지 않은 계정입니다. 추가 정보 입력 페이지로 이동합니다."
                ));
        User user = userRepository.findById(provider.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.SIGNUP_REQUIRED,
                        "가입되지 않은 계정입니다. 추가 정보 입력 페이지로 이동합니다."
                ));
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.PROFILE_NOT_FOUND,
                        "사용자 프로필을 찾을 수 없습니다."
                ));
        return authResult(user, profile);
    }

    public void withdraw(Long userId) {
        transactionPort.required(() -> {
            User user = userRepository.findById(userId)
                    .filter(User::isActive)
                    .orElseThrow(() -> new UserApplicationException(
                            UserErrorCode.USER_NOT_FOUND,
                            "존재하지 않는 사용자입니다."
                    ));
            userRepository.save(user.withdraw(LocalDateTime.now(clock)));
        });
    }

    private AuthResult authResult(User user, Profile profile) {
        String accessToken = accessTokenPort.issue(user.getId());
        return new AuthResult(
                accessToken,
                "Bearer",
                new AuthenticatedUser(user.getId(), profile.getNickname(), null)
        );
    }

    private void validateVerifiedEmail(GoogleIdentity identity) {
        if (identity == null || identity.subject() == null || identity.subject().isBlank()
                || identity.email() == null || identity.email().isBlank() || !identity.emailVerified()) {
            throw new UserApplicationException(
                    UserErrorCode.AUTH_FAILED_GOOGLE,
                    "계정 인증을 실패했습니다."
            );
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
