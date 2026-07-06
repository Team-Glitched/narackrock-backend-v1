package glitched.adlips.application.user.profile.usecase;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.domain.user.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileMuteToggleUseCase {

    private final ProfileRepositoryPort profileRepository;

    public ProfileMuteToggleUseCase(ProfileRepositoryPort profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional
    public boolean execute(Long userId) {
        Profile profile = profileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.PROFILE_NOT_FOUND, "존재하지 않는 사용자 프로필입니다."));
        Profile saved = profileRepository.save(profile.toggleMuted());
        return saved.isMuted();
    }
}
