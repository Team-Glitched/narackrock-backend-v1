package glitched.adlips.application.user.profile.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileFeaturedShortPort;

public class ProfileFeaturedShortUseCase {
    private final ProfileFeaturedShortPort featuredShorts;
    private final TransactionRunner transactions;

    public ProfileFeaturedShortUseCase(ProfileFeaturedShortPort featuredShorts, TransactionRunner transactions) {
        this.featuredShorts = featuredShorts;
        this.transactions = transactions;
    }

    public void execute(Long userId, Long shortId) {
        transactions.required(() -> {
            if (!featuredShorts.replaceFeaturedShort(userId, shortId)) {
                throw new UserApplicationException(UserErrorCode.VALIDATION_ERROR, "공개된 내 작업물이나 참가작만 대표 음악으로 설정할 수 있습니다.");
            }
            return null;
        });
    }
}
