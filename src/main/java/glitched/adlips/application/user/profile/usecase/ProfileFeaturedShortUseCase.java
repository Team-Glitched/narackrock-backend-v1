package glitched.adlips.application.user.profile.usecase;

import glitched.adlips.application.port.TransactionRunner;
import glitched.adlips.application.user.common.UserApplicationException;
import glitched.adlips.application.user.common.UserErrorCode;
import glitched.adlips.application.user.profile.port.out.ProfileFeaturedShortPort;

public class ProfileFeaturedShortUseCase {
    public static final int MAX_FEATURED_SHORTS = 5;

    private final ProfileFeaturedShortPort featuredShorts;
    private final TransactionRunner transactions;

    public ProfileFeaturedShortUseCase(ProfileFeaturedShortPort featuredShorts, TransactionRunner transactions) {
        this.featuredShorts = featuredShorts;
        this.transactions = transactions;
    }

    public void add(Long userId, Long shortId) {
        transactions.required(() -> {
            ProfileFeaturedShortPort.PinResult result =
                    featuredShorts.addFeaturedShort(userId, shortId, MAX_FEATURED_SHORTS);
            if (result == ProfileFeaturedShortPort.PinResult.NOT_ELIGIBLE) {
                throw new UserApplicationException(
                        UserErrorCode.VALIDATION_ERROR,
                        "공개된 내 작업물이나 참가작만 대표 음악으로 설정할 수 있습니다."
                );
            }
            if (result == ProfileFeaturedShortPort.PinResult.LIMIT_EXCEEDED) {
                throw new UserApplicationException(
                        UserErrorCode.VALIDATION_ERROR,
                        "대표 음악은 최대 5개까지 설정할 수 있습니다."
                );
            }
            return null;
        });
    }

    public void remove(Long userId, Long shortId) {
        transactions.required(() -> {
            featuredShorts.removeFeaturedShort(userId, shortId);
            return null;
        });
    }

    public void clear(Long userId) {
        transactions.required(() -> {
            featuredShorts.clearFeaturedShorts(userId);
            return null;
        });
    }
}
