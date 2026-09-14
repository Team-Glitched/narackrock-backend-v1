package glitched.adlips.application.user.profile.usecase;

import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;
import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.domain.user.Profile;
public class ProfileMuteToggleUseCase {

    private final ProfileRepositoryPort profileRepository;
    private final TransactionRunner transactionRunner;

    public ProfileMuteToggleUseCase(ProfileRepositoryPort profileRepository) {
        this(profileRepository, TransactionRunner.direct());
    }

    public ProfileMuteToggleUseCase(ProfileRepositoryPort profileRepository, TransactionRunner transactionRunner) {
        this.profileRepository = profileRepository;
        this.transactionRunner = transactionRunner;
    }

    public boolean execute(Long userId) {
        return transactionRunner.required(() -> executeInternal(userId));
    }

    private boolean executeInternal(Long userId) {
        Profile profile = profileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new UserApplicationException(
                        UserErrorCode.PROFILE_NOT_FOUND, "존재하지 않는 사용자 프로필입니다."));
        Profile saved = profileRepository.save(profile.toggleMuted());
        return saved.isMuted();
    }
}
