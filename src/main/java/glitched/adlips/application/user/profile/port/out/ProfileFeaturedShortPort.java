package glitched.adlips.application.user.profile.port.out;

public interface ProfileFeaturedShortPort {
    enum PinResult {
        PINNED,
        NOT_ELIGIBLE,
        LIMIT_EXCEEDED
    }

    PinResult addFeaturedShort(Long userId, Long shortId, int maxCount);

    void removeFeaturedShort(Long userId, Long shortId);

    void clearFeaturedShorts(Long userId);
}
