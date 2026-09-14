package glitched.adlips.application.user.profile.usecase;

import glitched.adlips.application.user.profile.dto.request.UserProfileShareRequest;
import glitched.adlips.application.user.profile.dto.response.UserProfileShareResponse;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileLinkPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.port.TransactionRunner;
public class ProfileShareUseCase {
    private final UserRepositoryPort userRepository;
    private final ProfileRepositoryPort profileRepository;
    private final ProfileLinkPort profileLinkPort;
    private final TransactionRunner transactionRunner;

    public ProfileShareUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            ProfileLinkPort profileLinkPort
    ) {
        this(userRepository, profileRepository, profileLinkPort, TransactionRunner.direct());
    }

    public ProfileShareUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            ProfileLinkPort profileLinkPort,
            TransactionRunner transactionRunner
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.profileLinkPort = profileLinkPort;
        this.transactionRunner = transactionRunner;
    }

    public UserProfileShareResponse execute(UserProfileShareRequest request) {
        return transactionRunner.readOnly(() -> executeInternal(request));
    }

    private UserProfileShareResponse executeInternal(UserProfileShareRequest request) {
        Long userId = request.userId();
        userRepository.findById(userId)
                .filter(user -> user.isActive())
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 사용자입니다."
                ));
        if (profileRepository.findByUserId(userId).isEmpty()) {
            throw new UserApplicationException(
                    UserErrorCode.PROFILE_NOT_FOUND,
                    "존재하지 않는 사용자 프로필입니다."
            );
        }
        return new UserProfileShareResponse(userId, profileLinkPort.create(userId));
    }
}
