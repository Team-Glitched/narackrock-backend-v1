package glitched.adlips.application.user.profile;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.common.port.out.UserRepositoryPort;
import glitched.adlips.application.user.profile.port.out.ProfileLinkPort;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProfileShareUseCase {
    private final UserRepositoryPort userRepository;
    private final ProfileRepositoryPort profileRepository;
    private final ProfileLinkPort profileLinkPort;

    public ProfileShareUseCase(
            UserRepositoryPort userRepository,
            ProfileRepositoryPort profileRepository,
            ProfileLinkPort profileLinkPort
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.profileLinkPort = profileLinkPort;
    }

    public ProfileShareResult execute(Long userId) {
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
        return new ProfileShareResult(userId, profileLinkPort.create(userId));
    }
}
