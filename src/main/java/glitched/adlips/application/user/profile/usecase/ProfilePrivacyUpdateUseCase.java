package glitched.adlips.application.user.profile.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileRepositoryPort;

public class ProfilePrivacyUpdateUseCase {
    private final ProfileRepositoryPort profiles;
    private final TransactionRunner transactions;
    public ProfilePrivacyUpdateUseCase(ProfileRepositoryPort profiles, TransactionRunner transactions) { this.profiles = profiles; this.transactions = transactions; }
    public boolean execute(Long userId, boolean privateProfile) {
        return transactions.required(() -> {
            var profile = profiles.findByUserId(userId).orElseThrow(() -> new UserApplicationException(UserErrorCode.PROFILE_NOT_FOUND, "프로필이 없습니다."));
            profiles.save(profile.changePrivacy(privateProfile));
            return privateProfile;
        });
    }
}
